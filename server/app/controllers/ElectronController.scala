package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.InjectedController

@Singleton
class ElectronController @Inject()() extends InjectedController {
	private val bootstrapFile = Seq(s"electron-opt.js", s"electron-fastopt.js")
		.find(name => getClass.getResource(s"/public/$name") != null)
		.map(name => routes.Assets.versioned(name).toString)

	def bootstrap = Action {
		Ok(Json.obj(
			"path" -> bootstrapFile
		))
	}
}
