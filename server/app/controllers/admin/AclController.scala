package controllers.admin

import base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController

@Singleton
class AclController @Inject()(userAction: UserAction) extends InjectedController {
	def users = userAction.authenticated { implicit req => NotImplemented }
	def groups = userAction.authenticated { implicit req => NotImplemented }
	def keys = userAction.authenticated { implicit req => NotImplemented }
}
