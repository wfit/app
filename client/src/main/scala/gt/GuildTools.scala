package gt

import facades.html5
import facades.electron.{ElectronModule, RemoteModule}
import org.scalajs.dom
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.Dynamic.{global => g}

@JSExportAll
@JSExportTopLevel("gt")
object GuildTools {
	val isApp: Boolean = g.GT_APP.asInstanceOf[Boolean]
	val isAuthenticated: Boolean = g.GT_AUTHENTICATED.asInstanceOf[Boolean]

	lazy val electron: ElectronModule = g.require("electron").asInstanceOf[ElectronModule]
	lazy val remote: RemoteModule = electron.remote

	def init(): Unit = {
		if (isApp) {
			setupWindowControls()
			setupCacheClearOnHide()
		}
		Interceptor.setup()
		Display.init()
	}

	private def setupWindowControls(): Unit = {
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

	private def setupCacheClearOnHide(): Unit = {
		dom.document.addEventListener("visibilitychange", (e: dom.Event) => {
			if (dom.document.hidden) electron.webFrame.clearCache()
		})
	}

	def reload(): Unit = {
		Display.beginLoading()
		dom.document.location.reload()
	}
}
