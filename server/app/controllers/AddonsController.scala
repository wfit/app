package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent}
import scala.concurrent.duration._
import services.EventBus

@Singleton
class AddonsController @Inject()(ws: WSClient, eventBus: EventBus)
                                (@NamedCache("ws") wsCache: AsyncCacheApi)
                                (cc: AppComponents) extends AppController(cc) {
	private val addonsAction = UserAction andThen CheckAcl("addons.access")

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
