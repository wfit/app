package gt

import facades.electron.{BrowserWindow, ElectronModule, LoginItemSettings, RemoteModule}
import facades.html5
import gt.workers.AutoWorker
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal, global => g}
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import utils.UserAcl

@JSExportAll
@JSExportTopLevel("gt")
object GuildTools {
	// Runtime constants
	lazy val isApp: Boolean = g.GT_APP.asInstanceOf[Boolean]
	lazy val isAuthenticated: Boolean = g.GT_AUTHENTICATED.asInstanceOf[Boolean]
	lazy val isMain: Boolean = !isDual && !isWorker
	lazy val isDual: Boolean = dom.window.navigator.userAgent contains "DualPanel"
	lazy val isWorker: Boolean = g.GT_MAIN.asInstanceOf[js.UndefOr[Boolean]].isEmpty
	lazy val stateHash: String = g.STATE_HASH.asInstanceOf[String]
	lazy val instanceLauncher: String = g.INSTANCE_LAUNCHER.asInstanceOf[String]
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
	lazy val root = remote.getGlobal[String]("electronRoot")

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
			if (isMain) {
				if (isAuthenticated) setupAppMenuToggle()
				setupCacheClearOnHide()
			}
			setupWindowControls()
			setupAutoLaunch(Settings.AppAutoLaunch.get)
		}
		Interceptor.setup()
		Tooltip.setup()
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
					case "devtools" => window.webContents.openDevTools(literal(mode = "detach"))
					case "reload" => reload()
					case "close" => window.close()
					case "quit" => remote.app.quit()
					case "restart" =>
						remote.app.relaunch()
						remote.app.quit()
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

	final type ?[T] = js.UndefOr[T]
	final val ? = js.undefined

	def open(url: String,
	         width: ?[Int] = 1200, height: ?[Int] = 700,
	         minWidth: ?[Int] = 1200, minHeight: ?[Int] = 700,
	         maxWidth: ?[Int] = ?, maxHeight: ?[Int] = ?): BrowserWindow = {

		val win = js.Dynamic.newInstance(GuildTools.remote.BrowserWindow)(js.Dynamic.literal(
			width = width,
			height = height,
			minWidth = minWidth,
			minHeight = minHeight,
			maxWidth = maxWidth,
			maxHeight = maxHeight,
			show = true,
			title = "Wait For It",
			icon = root + "/build/icon.ico",
			frame = false,
			autoHideMenuBar = true,
			backgroundColor = "#131313",
			webPreferences = js.Dynamic.literal(
				nodeIntegration = true,
				nodeIntegrationInWorker = true,
				devTools = true,
				textAreasAreResizable = false,
				defaultEncoding = "UTF-8"
			)
		)).asInstanceOf[BrowserWindow]
		win.webContents.setUserAgent(win.webContents.getUserAgent() + " DualPanel")
		url match {
			case absolute if url startsWith "http" => win.loadURL(absolute)
			case relative if url startsWith "/" => win.loadURL(dom.window.location.origin + relative)
		}
		win
	}

	def reload(target: js.UndefOr[String] = js.undefined): Unit = {
		Display.beginLoading()
		target.toOption match {
			case Some(path) => dom.document.location.href = path
			case None => dom.document.location.reload()
		}
	}
}
