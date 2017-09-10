package facades.electron

import scala.scalajs.js

@js.native
trait DialogModule extends js.Object {
	def showOpenDialog(win: BrowserWindow, options: js.Object): js.UndefOr[js.Array[String]] = js.native
	def showErrorBox(title: String, content: String): Unit = js.native
}
