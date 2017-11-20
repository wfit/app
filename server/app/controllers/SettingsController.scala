package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}

@Singleton
class SettingsController @Inject()(cc: AppComponents) extends AppController(cc) {
	def settings = UserAction.async { implicit req =>
		Ok(views.html.settings.settings())
	}
}
