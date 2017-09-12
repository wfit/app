package gt

import facades.html5
import facades.electron.{ElectronModule, RemoteModule}
import gt.workers.AutoWorker
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.Dynamic.{global => g}
import utils.UserAcl

@JSExportAll
@JSExportTopLevel("gt")
object GuildTools {
	// Runtime constants
	lazy val isApp: Boolean = g.GT_APP.asInstanceOf[Boolean]
	lazy val isAuthenticated: Boolean = g.GT_AUTHENTICATED.asInstanceOf[Boolean]
	lazy val isMain: Boolean = g.GT_MAIN.asInstanceOf[js.UndefOr[Boolean]] getOrElse false
	lazy val isWorker: Boolean = !isMain
	lazy val stateHash: String = g.STATE_HASH.asInstanceOf[String]
	lazy val instanceUUID: String = g.INSTANCE_UUID.asInstanceOf[String]
	lazy val clientScripts: js.Array[String] = g.CLIENT_SCRIPTS.asInstanceOf[js.Array[String]]
	lazy val sharedWorkers: js.Dictionary[String] = {
		g.SHARED_WORKERS.asInstanceOf[js.UndefOr[js.Dictionary[String]]] getOrElse js.Dictionary.empty[String]
	}
	lazy val acl: UserAcl = UserAcl(g.USER_ACL.asInstanceOf[js.Dictionary[Int]].toMap)

	// Electron bindings
	def require[T](module: String): T = g.require(module).asInstanceOf[T]
	lazy val electron: ElectronModule = require[ElectronModule]("electron")
	lazy val remote: RemoteModule = electron.remote

	def init(autoWorkers: js.Array[String]): Unit = {
		if (isApp) {
			setupWindowControls()
			setupCacheClearOnHide()
		}
		Interceptor.setup()
		AutoWorker.start(autoWorkers)
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

	def reload(target: js.UndefOr[String] = js.undefined): Unit = {
		Display.beginLoading()
		target.toOption match {
			case Some(path) => dom.document.location.href = path
			case None => dom.document.location.reload()
		}
	}
}
