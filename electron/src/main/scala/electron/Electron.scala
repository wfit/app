package electron

import scala.scalajs.js.Dynamic.{global => g}

object Electron {
	private val electron = g.require("electron")
	private val app = electron.app
	private val dialog = electron.dialog

	def main(args: Array[String]): Unit = {
		dialog.showErrorBox("Hello world", "\\o/")
		app.quit()
	}
}
