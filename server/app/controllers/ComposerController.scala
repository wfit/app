package controllers

import controllers.base.AppController
import java.time.LocalDateTime
import models.{Toons, Users}
import models.acl.AclView
import models.composer._
import play.api.libs.json.Json
import utils.UUID
import utils.SlickAPI._

class ComposerController extends AppController {
	private def ComposerAction = UserAction andThen CheckAcl("composer.access")

	def composer = ComposerAction.async { implicit req =>
		Documents.sortBy(doc => doc.updated.desc).result map (docs => Ok(views.html.composer.composer(docs)))
	}

	def create = ComposerAction { implicit req =>
		Ok(views.html.composer.create())
	}

	def createPost = ComposerAction(parse.json).async { implicit req =>
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

	def createFragment(doc: UUID) = ComposerAction(parse.json).async { implicit req =>
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
}
