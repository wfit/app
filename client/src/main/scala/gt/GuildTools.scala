package gt

import facades.html5
import facades.electron.{ElectronModule, LoginItemSettings, RemoteModule}
import gt.workers.AutoWorker
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import scala.scalajs.js.Dynamic.{literal, global => g}
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

	lazy val version: Int = if (!isApp) -1 else {
		g.GT_APP_VERSION.asInstanceOf[js.UndefOr[Int]] getOrElse buildVersionNumber
	}

	private def buildVersionNumber: Int = {
		remote.app.getVersion()
			.split('.')
			.map { part =>
				part.replaceAll("[^0-9]", "") match {
					case "" => 0
					case num => num.toInt
				}
			}
			.foldLeft(0)((prev, cur) => prev * 100 + cur)
	}

	def init(autoWorkers: js.Array[String]): Unit = {
		if (isApp) {
			setupAppMenuToggle()
			setupWindowControls()
			setupCacheClearOnHide()
			setupAutoLaunch(Settings.AppAutoLaunch.get)
		}
		Interceptor.setup()
		AutoWorker.start(autoWorkers)
		Display.init()
	}

	private def setupAppMenuToggle(): Unit = {
		val nav = dom.document.getElementById("app-nav")
		dom.document.querySelector("header h1").addEventListener("click", (e: dom.MouseEvent) => {
			nav.classList.add("open")
		})
		nav.addEventListener("mouseleave", (e: dom.MouseEvent) => {
			nav.classList.remove("open")
		})
	}

	private def setupWindowControls(): Unit = {
		dom.document.querySelector("header").addEventListener("click", (e: dom.MouseEvent) => {
			lazy val window = remote.getCurrentWindow()
			val target = e.target.asInstanceOf[html5.Element].closest("[data-action]")
			if (target ne null) {
				target.asInstanceOf[html5.HTMLElement].dataset.action.get match {
					case "minimize" => window.minimize()
					case "maximize" if window.isMaximized() => window.unmaximize()
					case "maximize" => window.maximize()
					case "close" => window.close()
					case "devtools" => window.webContents.openDevTools(literal(mode = "detach"))
					case "reload" => reload()
				}
			}
		})
	}

	private def setupCacheClearOnHide(): Unit = {
		dom.document.addEventListener("visibilitychange", (e: dom.Event) => {
			if (dom.document.hidden) electron.webFrame.clearCache()
		})
	}

	private[gt] def setupAutoLaunch(state: Boolean): Unit = {
		val current = remote.app.getLoginItemSettings(LoginItemSettings(args = js.Array("--at-login")))
		if (current.openAtLogin != state) {
			remote.app.setLoginItemSettings(LoginItemSettings(
				openAtLogin = state,
				args = js.Array("--at-login")
			))
		}
	}

	def reload(target: js.UndefOr[String] = js.undefined): Unit = {
		Display.beginLoading()
		target.toOption match {
			case Some(path) => dom.document.location.href = path
			case None => dom.document.location.reload()
		}
	}
}
