package electron

import facades.electron.Tray
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, newInstance => jnew}

object AppTray {
	val icon: String = Electron.root + "/build/icon.ico"

	private val instance = jnew(Electron.moduleDynamic.Tray)(icon).asInstanceOf[Tray]

	private val openDevTools: js.Function = () => Window.openDevTools()
	private val reloadWindow: js.Function = () => Window.reload()

	private val menu = Electron.menu.buildFromTemplate(js.Array(
		lit(label = "DevTools", click = openDevTools),
		lit(label = "Recharger", click = reloadWindow),
		lit(`type` = "separator"),
		lit(label = "Quitter", role = "quit")
	))

	def setup(): Unit = {
		instance.setToolTip("Wait For It")
		instance.setContextMenu(menu)
		instance.on("click", () => Window.show())
	}
}
