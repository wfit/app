package controllers

import akka.stream.Materializer
import controllers.base.{CheckAcl, UserAction}
import javax.inject.{Inject, Singleton}
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, InjectedController}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import services.EventBus

@Singleton
class AddonsController @Inject()(userAction: UserAction, checkAcl: CheckAcl, ws: WSClient)
                                (eventBus: EventBus)
                                (@NamedCache("ws") wsCache: AsyncCacheApi)
                                (implicit ec: ExecutionContext, mat: Materializer)
	extends InjectedController {

	private val addonsAction = userAction andThen checkAcl("addons.access")

	def list = addonsAction { implicit req =>
		Ok(views.html.addons.list())
	}

	private def proxyRequest(key: String, url: String, mime: String,
	                         immutable: Boolean = false): Action[AnyContent] = {
		addonsAction.async {
			wsCache.getOrElseUpdate(key, 15.minutes) {
				ws.url(url).get().collect {
					case res if res.status == 200 =>
						Ok(res.bodyAsBytes).as(mime).withHeaders(
							"Cache-Control" -> {
								if (immutable) "public, max-age=3600, immutable"
								else "no-cache, no-store, must-revalidate"
							}
						)
				}
			}.recover { case _ => NotFound }
		}
	}

	def manifest = proxyRequest(
		"addons:manifest",
		"http://addons.wfit.ovh/manifest.php",
		"application/json"
	)

	def digest(addon: String) = proxyRequest(
		s"addons:digest:$addon",
		s"http://addons.wfit.ovh/repository/$addon.digest.json",
		"application/json"
	)

	def blob(hash: String) = proxyRequest(
		s"addons:blob:$hash",
		s"http://addons.wfit.ovh/blobs/$hash",
		"application/octet-stream",
		immutable = true
	)

	def notifyUpdate = Action.async {
		wsCache.removeAll().map(_ => Ok) andThen { case _ => eventBus.publish("updater.notify", ()) }
	}
}
