package electron

import facades.electron.AutoUpdaterModule
import scala.scalajs.js.Dynamic.{global => g}

object AutoUpdate {
	private lazy val autoUpdater = g.require("electron-updater").autoUpdater.asInstanceOf[AutoUpdaterModule]

	def setup(): Unit = if (!Electron.dev) {
		autoUpdater.removeAllListeners("update-not-available")
		autoUpdater.removeAllListeners("update-downloaded")
	}
}
