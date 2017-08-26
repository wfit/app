package controllers

import base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController

@Singleton
class DashboardController @Inject()(userAction: UserAction) extends InjectedController {
	def dashboard = userAction.authenticated {
		Ok("Dashboard")
	}
}
