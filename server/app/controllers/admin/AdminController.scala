package controllers.admin

import base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController

@Singleton
class AdminController @Inject()(userAction: UserAction) extends InjectedController {
	def home = userAction.authenticated { implicit req => NotImplemented }
}
