package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}

@Singleton
class DashboardController @Inject()(cc: AppComponents) extends AppController(cc) {
	def dashboard = UserAction.authenticated { implicit req =>
		Ok(views.html.dashboard.dashboard())
	}
}
