package controllers

import akka.http.scaladsl.model.StatusCodes.CustomStatusCode
import base.UserAction
import javax.inject._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import services.AuthService
import utils.CustomStatus

@Singleton
class HomeController @Inject()(userAction: UserAction)
                              (authService: AuthService)
                              (implicit executionContext: ExecutionContext)
	extends InjectedController {

	def index = userAction { req =>
		if (req.authenticated) Redirect(routes.DashboardController.dashboard())
		else Redirect(routes.HomeController.login())
	}

	def login(continue: Option[String]) = userAction.unauthenticated { implicit req =>
		Ok(views.html.home.login())
	}

	def performLogin = userAction.unauthenticated(parse.json).async { req =>
		val id = (req.body \ "id").as[String]
		val pass = (req.body \ "pass").as[String]
		authService.login(id, pass).flatMap(authService.createSession).map { id =>
			Redirect(routes.DashboardController.dashboard(), CustomStatus.FullRedirect).withSession("key" -> id.value)
		}
	}

	def logout = Action {
		Redirect(routes.HomeController.index()).withNewSession
	}
}
