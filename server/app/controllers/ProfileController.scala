package controllers

import base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController
import scala.concurrent.ExecutionContext
import services.{BnetService, RosterService}
import utils.{FutureOps, UserError, UUID}

@Singleton
class ProfileController @Inject()(userAction: UserAction)
                                 (bnetService: BnetService, rosterService: RosterService)
                                 (implicit executionContext: ExecutionContext)
	extends InjectedController {

	private val unknownUser = UserError("Ce joueur n'existe pas.")
	private val alreadyBoundToon = UserError("Ce personnage est déjà associé à un compte utilisateur.")

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

	def edit(user: UUID) = userAction.authenticated { implicit req =>
		Ok(views.html.profile.edit())
	}

	def bind(user: UUID) = userAction.authenticated { implicit req =>
		Ok(views.html.profile.bind(user))
	}

	def bindPost(user: UUID) = userAction.authenticated(parse.json).async { implicit req =>
		for {
			toon <- bnetService.fetchToon(req.param("realm").asString, req.param("name").asString)
			_ <- rosterService.bindToon(user, toon).replaceFailure(alreadyBoundToon)
		} yield Redirect(routes.ProfileController.profile(user))
	}
}
