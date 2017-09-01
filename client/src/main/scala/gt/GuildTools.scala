package gt

import facades.html5
import facades.electron.ElectronModule
import org.scalajs.dom
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.Dynamic.{global => g}

@JSExportAll
@JSExportTopLevel("gt")
object GuildTools {
	val app: Boolean = g.GT_APP.asInstanceOf[Boolean]
	val authenticated: Boolean = g.GT_AUTHENTICATED.asInstanceOf[Boolean]

	private lazy val electron = g.require("electron").asInstanceOf[ElectronModule]
	private lazy val remote = electron.remote

	def init(): Unit = {
		setupWindowControls()
		Interceptor.setup()
		Display.init()
	}

	private def setupWindowControls(): Unit = if (app) {
		dom.document.getElementById("app-buttons").addEventListener("click", (e: dom.MouseEvent) => {
			lazy val window = remote.getCurrentWindow()
			val target = e.target.asInstanceOf[html5.Element].closest("[data-action]")
			if (target ne null) {
				target.asInstanceOf[html5.HTMLElement].dataset.action.get match {
					case "minimize" => window.minimize()
					case "maximize" if window.isMaximized() => window.unmaximize()
					case "maximize" => window.maximize()
					case "close" => window.close()
				}
			}
		})
	}

	def reload(): Unit = {
		Display.beginLoading()
		dom.document.location.reload()
	}
}
