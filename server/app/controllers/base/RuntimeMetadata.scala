package controllers.base

import play.api.libs.json.JsValue
import play.api.libs.json.Json.JsValueWrapper
import scala.concurrent.Future
import services.RuntimeService
import utils.UUID

class RuntimeMetadata (service: RuntimeService) {
	def instanceUUID: UUID = service.instanceUUID

	val clientScripts: Seq[String] = Seq(
		service.clientDependencies.get,
		service.clientScript.get
	)
}

object RuntimeMetadata {
	private var cache: Option[(RuntimeService, Future[RuntimeMetadata])] = None

	def fromService(runtimeService: RuntimeService): Future[RuntimeMetadata] = {
		cache match {
			case Some((service, future)) if service == runtimeService =>
				future
			case _ =>
				val future = Future.successful { new RuntimeMetadata(runtimeService) }
				cache = Some((runtimeService, future))
				future
		}
	}
}
