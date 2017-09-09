package controllers

import base.{CheckAcl, UserAction}
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController

@Singleton
class AddonsController @Inject()(userAction: UserAction, checkAcl: CheckAcl) extends InjectedController {
	private val addonsAction = userAction andThen checkAcl("addons.access")

	def list = addonsAction { implicit req =>
		Ok(views.html.addons.list())
	}
}
