package electron

import facades.electron._
import facades.node.EventEmitter
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object Electron {
	val moduleDynamic: js.Dynamic = g.require("electron")
	val module: ElectronModule = moduleDynamic.asInstanceOf[ElectronModule]
	val dev: Boolean = g.require("electron-is-dev").asInstanceOf[Boolean]

	val app: AppModule = module.app
	val dialog: DialogModule = module.dialog
	val menu: MenuModule = module.Menu

	val root: String = g.electronRoot.asInstanceOf[String]
	val base: String = g.appBase.asInstanceOf[String]

	def main(args: Array[String]): Unit = {
		AppTray.setup()
		AutoUpdate.setup()
		Window.setup()
		g.closeSplash()
		g.disableWatchdog()
	}
}
