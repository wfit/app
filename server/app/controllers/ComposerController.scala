package controllers

import controllers.base.{AppController, UserRequest}
import gt.modules.composer.ComposerUtils
import java.time.LocalDateTime
import javax.inject.Inject
import models.{Toons, Users}
import models.acl.AclView
import models.composer._
import play.api.libs.json.Json
import play.twirl.api.Html
import scala.concurrent.Future
import scala.util.{Success, Try}
import services.EventBus
import slick.jdbc.TransactionIsolation
import utils.UUID
import utils.JsonFormats._
import utils.SlickAPI._

class ComposerController @Inject() (eventBus: EventBus) extends AppController {
	private def ComposerAction = UserAction andThen CheckAcl("composer.access")
	private def ComposerEditAction = UserAction andThen CheckAcl("composer.access", "composer.edit")

	/** The main composer view */
	def composer = ComposerAction.async { implicit req =>
		Documents.sortBy(doc => doc.updated.desc).result map (docs => Ok(views.html.composer.composer(docs)))
	}

	/** Create a new composer document */
	def create = ComposerEditAction { implicit req =>
		Ok(views.html.composer.create())
	}

	/** Form handler for new composer document creation */
	def createPost = ComposerEditAction(parse.json).async { implicit req =>
		val title = req.param("title").asString
		val doc = Document(UUID.random, title, LocalDateTime.now)
		(Documents += doc) andThen Redirect(routes.ComposerController.composer())
	}

	/** Rendered HTML block for a fragment */
	private def fragmentHtml(fragment: Fragment)(implicit req: UserRequest[_]): Future[Html] = fragment.style match {
		case Fragment.Text =>
			Future.successful(views.html.composer.text(fragment))
		case Fragment.Group =>
			val query = Slots filter (s => s.fragment === fragment.id) joinLeft Toons on ((s, t) => s.toon === t.uuid)
			query.result.run.map { slots =>
				views.html.composer.group(fragment, slots.sorted(ComposerUtils.StandardSlotToonOrdering))
			}
		case Fragment.Grid =>
			Future.successful(views.html.composer.grid(fragment))
	}

	/** Composer document sub-page */
	def document(id: UUID) = ComposerAction.async { implicit req =>
		for {
			doc <- Documents.filter(doc => doc.id === id).result.head
			frags <- Fragments.filter(f => f.doc === id).sortBy(_.sort.asc).result
			html <- DBIO.from(Future.sequence(frags.map(fragmentHtml)))
		} yield {
			Ok(views.html.composer.document(doc, html))
		}
	}

	/** The composer editor */
	def editor(id: UUID) = ComposerEditAction.async { implicit req =>
		Documents.filter(doc => doc.id === id).result.headOption map {
			case Some(doc) => Ok(views.html.composer.editor(doc))
			case None => NotFound("Ce document n'existe pas")
		}
	}

	/** The roster sidebar content */
	def roster = ComposerEditAction.async { implicit req =>
		(for {
			userId <- AclView.queryKey("composer.include")(_ > 0)
			user <- Users.filter(u => u.uuid === userId)
			rank <- AclView.granted(userId, "rank")
			toon <- Toons.filter(t => t.active && t.owner === userId)
		} yield (user, rank, toon)).result.map { res =>
			val entries = res.map((RosterEntry.apply _).tupled)
			Ok(Json.toJson(entries))
		}
	}

	/** The list of fragments for a document */
	def fragments(doc: UUID) = ComposerEditAction.async { implicit req =>
		Fragments.filter(f => f.doc === doc).sortBy(_.sort).result.map(frags => Ok(Json.toJson(frags)))
	}

	/** Updates the last modification time of a document */
	private def touchDocument(doc: UUID): PartialFunction[Try[_], Unit] = {
		case _ => Documents.filter(d => d.id === doc).map(_.updated).update(LocalDateTime.now).run
	}

	/** Creates a new fragment in a document */
	def createFragment(doc: UUID) = ComposerEditAction(parse.json).async { implicit req =>
		val uuid = UUID.random
		val sort = Fragments.filter(f => f.doc === doc).map(_.sort).max.getOrElse(0) + 1
		val style = Fragment.styleFromString(req.param("style").asString)
		val title = style match {
			case Fragment.Text => "Texte"
			case Fragment.Group => "Groupe"
			case Fragment.Grid => "Grid"
		}

		val shape = Fragments.map(f => (f.id, f.doc, f.sort, f.style, f.title))
		val insert = shape forceInsertExpr (uuid, doc, sort, style, title)
		val action = (insert andThen Fragments.filter(f => f.id === uuid).result.head).transactionally andThen Created

		action.run andThen touchDocument(doc) andThen {
			case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
		}
	}

	/** Moves a fragment in the document */
	def moveFragment(doc: UUID) = ComposerEditAction(parse.json).async { implicit req =>
		val source = req.param("source").asUUID
		val target = req.param("target").asUUID
		require(source != target)

		val position = req.param("position").asString
		val frags = Fragments.filter(f => f.doc === doc)

		val locations = for {
			sourcePos <- frags.filter(f => f.id === source).map(f => f.sort).result.head
			targetPos <- frags.filter(f => f.id === target).map(f => f.sort).result.head
		} yield (sourcePos, targetPos)

		val action = locations.flatMap { case (sourcePos, targetPos) =>
			val (range, offset, pos) = (sourcePos, targetPos, position) match {
				case (s, t, "before") if s < t => ((s + 1) until t, -1, t - 1)
				case (s, t, "after") if s < t => ((s + 1) to t, -1, t)
				case (s, t, "before") if s > t => ((s - 1) to t by -1, 1, t)
				case (s, t, "after") if s > t => ((s - 1) until t by -1, 1, t + 1)
			}

			if (pos != sourcePos) {
				DBIO.sequence(for (i <- range) yield {
					frags.filter(f => f.sort === i).map(f => f.sort).update(i + offset)
				}) andThen frags.filter(f => f.id === source).map(f => f.sort).update(pos)
			} else {
				// Nothing to do if moving fragment before its next neighbor or after its previous one
				DBIO.successful(0)
			}
		} andThen Ok

		action.transactionally.run andThen touchDocument(doc) andThen {
			case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
		}
	}

	/** Renames a fragment */
	def renameFragment(doc: UUID, frag: UUID) = ComposerEditAction(parse.text).async { implicit req =>
		val title = req.body
		if (title matches """^\s*$""") {
			UnprocessableEntity("Titre non-acceptable")
		} else {
			val action = Fragments.filter(f => f.id === frag && f.doc === doc).map(_.title).update(title) andThen Ok
			action.run andThen touchDocument(doc) andThen {
				case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
			}
		}
	}

	/** Deletes a fragment */
	def deleteFragment(doc: UUID, frag: UUID) = ComposerEditAction.async { implicit req =>
		val query = Fragments.filter(f => f.id === frag && f.doc === doc)
		val maxSort = Fragments.filter(f => f.doc === doc).map(_.sort).max.result
		(query.result.head zip maxSort).flatMap { case (frag, max) =>
			val moveActions = (frag.sort + 1 to max.get).map { i =>
				Fragments.filter(f => f.doc === doc && f.sort === i).map(_.sort).update(i - 1)
			}
			DBIO.seq(
				query.delete,
				DBIO.sequence(moveActions)
			).transactionally andThen Ok
		}.run andThen touchDocument(doc) andThen {
			case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
		}
	}

	/** The set of valid fragments for a document, used to prevent cross-document updates */
	private def validFragmentsForDoc(doc: UUID): Query[Rep[UUID], UUID, Seq] = {
		Fragments.filter(f => f.doc === doc).map(_.id)
	}

	/** The set of slots for a given document and fragment */
	private def slotsForDocAndFrag(doc: UUID, frag: UUID): Query[Slots, Slot, Seq] = {
		Slots.filter(s => s.fragment === frag && (s.fragment in validFragmentsForDoc(doc)))
	}

	/** Retrieves the set of slots for a given fragment */
	def getSlots(doc: UUID, frag: UUID) = ComposerEditAction.async { implicit req =>
		slotsForDocAndFrag(doc, frag)
			.joinLeft(Toons).on((gs, t) => gs.toon === t.uuid)
			.result
			.map(res => Ok(Json.toJson(res)))
	}

	/** Set a slot in a fragment */
	def setSlot(doc: UUID, frag: UUID) = ComposerEditAction(parse.json).async { implicit req =>
		// Get target slot `row` and `col` values, col can be null
		val row = req.param("row").asInt
		val col = req.param("col").asOpt[Int]

		// The request should include either a `toon` property or a `slot` indicating
		// the source of data for this slot. If a toon was given, a new slot is created
		// based on the toons attributes. If a slot was given, the slot is updated.
		(req.param("toon").asOpt[UUID] match {
			case Some(toonUUID) =>
				Toons.filter(t => t.uuid === toonUUID).result.head.map { toon =>
					(Some(toon.uuid), toon.name, toon.spec.role, toon.cls, None, None)
				}
			case None =>
				Slots.filter(s => s.fragment in validFragmentsForDoc(doc))
					.filter(s => s.id === req.param("slot").asUUID)
					.forUpdate.result.head
					.map { slot =>
						(slot.toon, slot.name, slot.role, slot.cls, Some(slot.id), Some(slot.fragment))
					}
		}).flatMap { case (toon, name, role, cls, id, source) =>
			// Only manipulate slots owned by the target fragment
			val slots = Slots.filter(s => s.fragment === frag)

			// Remove slot with duplicate toon
			val duplicateToonRemove = toon match {
				case Some(uuid) => slots.filter(cs => cs.toon === uuid).delete
				case None => DBIO.successful(0)
			}

			// Remove slot with duplicate location
			val duplicateLocationRemove = (row, col) match {
				case (r, Some(c)) => slots.filter(cs => cs.row === r && cs.col === c).delete
				case _ => DBIO.successful(0)
			}

			// Insert the new slot in the database
			val copy = req.param("copy").asOpt[Boolean] getOrElse false
			val slotId = id filterNot (_ => copy) getOrElse UUID.random
			val slotInsert = Slots insertOrUpdate Slot(
				slotId, frag, row, col getOrElse -1,
				toon, name, role, cls
			)

			// The whole operation
			DBIO.seq(
				duplicateToonRemove,
				duplicateLocationRemove,
				slotInsert
			) andThen source
		}.transactionally.withTransactionIsolation(TransactionIsolation.Serializable).run andThen {
			case Success(source) =>
				eventBus.publish(s"composer:$doc:fragment.update", frag)
				for (s <- source) eventBus.publish(s"composer:$doc:fragment.update", s)
		} map (_ => Ok) andThen touchDocument (doc)
	}

	/** Delete a slot in a fragment */
	def deleteSlot(doc: UUID, frag: UUID, slot: UUID) = ComposerEditAction.async { implicit req =>
		val action = slotsForDocAndFrag(doc, frag).filter(s => s.id === slot).delete andThen Ok
		action.run andThen touchDocument(doc) andThen {
			case _ => eventBus.publish(s"composer:$doc:fragment.update", frag)
		}
	}
}
