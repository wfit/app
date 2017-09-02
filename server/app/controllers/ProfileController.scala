package controllers

import base.{UserAction, UserRequest}
import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneId}
import java.util.TimeZone
import javax.inject.{Inject, Singleton}
import models.{Toon, Toons}
import models.wow.Spec
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import services.{BnetService, RosterService}
import utils.{FutureOps, UserError, UUID}
import utils.SlickAPI._

@Singleton
class ProfileController @Inject()(userAction: UserAction)
                                 (bnetService: BnetService, rosterService: RosterService)
                                 (implicit executionContext: ExecutionContext)
	extends InjectedController {

	private val unknownUser = UserError("Ce joueur n'existe pas.")
	private val alreadyBoundToon = UserError("Ce personnage est déjà associé à un compte utilisateur.")

	private case class EditPermissionFilter (user: UUID) extends ActionFilter[UserRequest] {
		protected def executionContext: ExecutionContext = ProfileController.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (user == request.user.uuid || request.acl.can("profile.edit")) Future.successful(None)
			else Future.successful(Some(Forbidden("Vous n'êtes pas autorisé à modifier ce profil.")))
		}
	}

	private def editAction(user: UUID) = userAction.authenticated andThen EditPermissionFilter(user)

	def autoProfile = userAction.authenticated { req =>
		Redirect(routes.ProfileController.profile(req.user.uuid))
	}

	def profile(user: UUID) = userAction.authenticated.async { implicit req =>
		val data = rosterService.loadUser(user).collect {
			case Some(u) => u
			case None => throw unknownUser
		}
		val toons = rosterService.toonsForUser(user)
		for (d <- data; t <- toons; m <- rosterService.mainForUser(d)) yield {
			Ok(views.html.profile.profile(d, m, t))
		}
	}

	def edit(user: UUID) = editAction(user) { implicit req =>
		Ok(views.html.profile.edit())
	}

	def bind(user: UUID) = editAction(user) { implicit req =>
		Ok(views.html.profile.bind(user))
	}

	def bindPost(user: UUID) = editAction(user)(parse.json).async { implicit req =>
		for {
			toon <- bnetService.fetchToon(req.param("realm").asString, req.param("name").asString)
			_ <- rosterService.bindToon(user, toon).replaceFailure(alreadyBoundToon)
		} yield Redirect(routes.ProfileController.profile(user))
	}

	private def mutateToon(user: UUID, toon: UUID)
	                      (impl: Query[Toons, Toon, Seq] => DBIOAction[_, NoStream, Nothing]): Action[AnyContent] = {
		editAction(user).async {
			val query = Toons.filter(t => t.uuid === toon && t.owner === user)
			val action = impl(query) andThen Redirect(routes.ProfileController.profile(user))
			action.run andThenAsync rosterService.flushUserCaches(user)
		}
	}

	def toonDisable(user: UUID, toon: UUID) = mutateToon(user, toon) { query =>
		query.filter(_.main === false).map(_.active).update(false)
	}

	def toonEnable(user: UUID, toon: UUID) = mutateToon(user, toon) { query =>
		query.map(_.active).update(true)
	}

	def toonPromote(user: UUID, toon: UUID) = mutateToon(user, toon) { query =>
		val demote = Toons.filter(t => t.owner === user && t.main === true).map(_.main).update(false).filter(_ == 1)
		val promote = query.map(t => (t.main, t.active)).update((true, true)).filter(_ == 1)
		(demote andThen promote).transactionally
	}

	def toonUpdate(user: UUID, toon: UUID) = editAction(user).async {
		// FIXME: no permission check and vulnerable to request forgery
		rosterService.updateToon(toon).replaceSuccess(Redirect(routes.ProfileController.profile(user)))
	}

	def toonRemove(user: UUID, toon: UUID) = mutateToon(user, toon) { query =>
		query.filter(_.main === false).delete
	}

	def toonSetSpec(user: UUID, toon: UUID, specId: Int) = mutateToon(user, toon) { query =>
		val spec = Spec.fromId(specId)
		query.filter(t => t.cls === spec.cls).map(_.spec).update(spec)
	}
}
