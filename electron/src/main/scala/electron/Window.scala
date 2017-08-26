package electron

import facades.electron.BrowserWindow
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, newInstance => jnew}

object Window {
	private lazy val win: BrowserWindow = jnew(Electron.moduleDynamic.BrowserWindow)(lit(
		width = 1200,
		height = 700,
		minWidth = 1200,
		minHeight = 700,
		show = false,
		title = "Wait For It",
		icon = AppTray.icon,
		frame = false,
		autoHideMenuBar = true,
		backgroundColor = "#121212"
	)).asInstanceOf[BrowserWindow]

	private var isVisible = false
	private var isReady = false

	def setup(): Unit = {
		win.loadURL(Electron.base)

		win.once("ready-to-show", () => {
			isReady = true
			if (isVisible) win.show()
		})

		win.on("close", (event: js.Dynamic) => {
			event.preventDefault()
			win.hide()
			isVisible = false
		})

		Electron.app.on("before-quit", () => close())
	}

	def show(): Unit = {
		isVisible = true
		if (isReady) win.show()
	}

	def close(): Unit = {
		win.destroy()
	}
}
