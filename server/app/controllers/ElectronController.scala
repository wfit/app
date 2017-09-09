package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.InjectedController
import services.RuntimeService

@Singleton
class ElectronController @Inject()(runtimeService: RuntimeService) extends InjectedController {
	def bootstrap = Action {
		Ok(Json.obj(
			"path" -> runtimeService.bootstrapScript
		))
	}
}
