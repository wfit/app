package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import services.RuntimeService

@Singleton
class ElectronController @Inject()(runtimeService: RuntimeService)
                                  (cc: AppComponents) extends AppController(cc) {
	def bootstrap = Action {
		Ok(Json.obj(
			"path" -> runtimeService.bootstrapScript
		))
	}
}
