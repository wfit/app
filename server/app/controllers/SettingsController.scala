package controllers

import controllers.base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.mvc.InjectedController

@Singleton
class SettingsController @Inject()(userAction: UserAction) extends InjectedController {
	def settings = userAction.async { implicit req =>
		Ok(views.html.settings.settings())
	}
}
