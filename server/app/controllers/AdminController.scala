package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}

@Singleton
class AdminController @Inject()(cc: AppComponents) extends AppController(cc) {
	def home = UserAction.authenticated { implicit req =>
		Ok(views.html.admin.home())
	}
}
