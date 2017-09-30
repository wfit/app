package controllers

import controllers.base.AppController
import java.time.LocalDateTime
import javax.inject.Inject
import models.{Toons, Users}
import models.acl.AclView
import models.composer._
import play.api.libs.json.Json
import services.EventBus
import utils.UUID
import utils.SlickAPI._

class ComposerController @Inject() (eventBus: EventBus) extends AppController {
	private def ComposerAction = UserAction andThen CheckAcl("composer.access")
	private def ComposerEditAction = UserAction andThen CheckAcl("composer.access", "composer.edit")

	def composer = ComposerAction.async { implicit req =>
		Documents.sortBy(doc => doc.updated.desc).result map (docs => Ok(views.html.composer.composer(docs)))
	}

	def create = ComposerEditAction { implicit req =>
		Ok(views.html.composer.create())
	}

	def createPost = ComposerEditAction(parse.json).async { implicit req =>
		val title = req.param("title").asString
		val doc = Document(UUID.random, title, LocalDateTime.now())
		(Documents += doc) andThen Redirect(routes.ComposerController.composer())
	}

	def roster = ComposerAction.async { implicit req =>
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

	def document(id: UUID) = ComposerAction.async { implicit req =>
		Documents.filter(doc => doc.id === id).result.headOption map {
			case Some(doc) => Ok(views.html.composer.document(doc))
			case None => NotFound("Document non-existant")
		}
	}

	def fragments(doc: UUID) = ComposerAction.async { implicit req =>
		Fragments.filter(f => f.doc === doc).sortBy(_.sort).result.map(frags => Ok(Json.toJson(frags)))
	}

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
		val action = (insert andThen Fragments.filter(f => f.id === uuid).result.head).transactionally

		action andThen Created
	}

	def move(doc: UUID) = ComposerEditAction(parse.json).async { implicit req =>
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

		action.transactionally.run andThen {
			case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
		}
	}

	def renameFragment(doc: UUID) = ComposerEditAction(parse.json).async { implicit req =>
		val fragment = req.param("fragment").asUUID
		val title = req.param("title").asString

		if (title matches """^\s*$""") {
			UnprocessableEntity("Titre non-acceptable")
		} else {
			val action = Fragments.filter(f => f.id === fragment && f.doc === doc).map(_.title).update(title) andThen Ok
			action.run andThen {
				case _ => eventBus.publish(s"composer:$doc:fragments.refresh", ())
			}
		}
	}
}
