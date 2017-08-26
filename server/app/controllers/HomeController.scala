package controllers

import base.UserAction
import javax.inject._
import play.api.mvc._

@Singleton
class HomeController @Inject()(userAction: UserAction) extends InjectedController {
	def index = userAction { req =>
		if (req.authenticated) Redirect(routes.DashboardController.dashboard())
		else Redirect(routes.HomeController.login())
	}

	def login(continue: Option[String]) = userAction.unauthenticated { implicit req =>
		Ok(views.html.home.login())
	}

	def logout = Action {
		Redirect(routes.HomeController.index()).withNewSession
	}
}
