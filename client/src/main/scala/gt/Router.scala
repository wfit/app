package gt

import scala.language.{dynamics, implicitConversions}
import scala.scalajs.js

object Router extends Dynamic {
  private val routes = js.Dynamic.global.routes

  //def selectDynamic(ctrl: String): Controller = Controller(ctrl)
  val Assets       = Controller("Assets")
  val Addons       = Controller("AddonsController")
  val Admin        = Controller("AdminController")
  val Availability = Controller("AvailabilityController")
  val Composer     = Controller("ComposerController")
  val Dashboard    = Controller("DashboardController")
  val Electron     = Controller("ElectronController")
  val EventBus     = Controller("EventBusController")
  val Home         = Controller("HomeController")
  val Profile      = Controller("ProfileController")
  val Roster       = Controller("RosterController")
  val Settings     = Controller("SettingsController")

  case class Controller(ctrl: String) extends Dynamic {
    def applyDynamic(endpoint: String)(args: Any*): Route = {
      val effectiveArgs = args.map(arg => arg.toString)
      Route(routes.controllers.selectDynamic(ctrl).applyDynamic(endpoint)(effectiveArgs.asInstanceOf[Seq[js.Any]]: _*))
    }

    def selectDynamic(ctrl: String): Route = applyDynamic(ctrl)()
  }

  case class Route(route: js.Dynamic) {
    def url: String          = route.url.asInstanceOf[String]
    def method: String       = route.method.asInstanceOf[String]
    def absoluteURL: String  = route.absoluteURL().asInstanceOf[String]
    def webSocketURL: String = route.webSocketURL().asInstanceOf[String]
  }

  implicit def RouteToString(route: Route): String = route.url
}
