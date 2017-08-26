package electron

import facades.electron.Tray
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{literal => lit, newInstance => jnew}

object AppTray {
	val icon: String = Electron.root + "/build/icon.ico"

	private val instance = jnew(Electron.moduleDynamic.Tray)(icon).asInstanceOf[Tray]

	private val menu = Electron.menu.buildFromTemplate(js.Array(
		lit(label = "Quitter", role = "quit")
	))

	def setup(): Unit = {
		instance.setToolTip("Wait For It")
		instance.setContextMenu(menu)
		instance.on("click", () => Window.show())
	}
}
