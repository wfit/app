package gt

import scala.language.{dynamics, implicitConversions}
import scala.scalajs.js

object Router extends Dynamic {
	private val routes = js.Dynamic.global.routes

	def selectDynamic(ctrl: String): Controller = Controller(ctrl)

	case class Controller (ctrl: String) extends Dynamic {
		def applyDynamic(endpoint: String)(args: js.Any*): Route = {
			Route(routes.controllers.selectDynamic(ctrl).applyDynamic(endpoint)(args: _*))
		}

		def selectDynamic(ctrl: String): Route = applyDynamic(ctrl)()
	}

	case class Route (route: js.Dynamic) {
		def url: String = route.url.asInstanceOf[String]
		def method: String = route.method.asInstanceOf[String]
		def absoluteURL: String = route.absoluteURL().asInstanceOf[String]
		def webSocketURL: String = route.webSocketURL().asInstanceOf[String]
	}

	implicit def RouteToString(route: Route): String = route.url
}
