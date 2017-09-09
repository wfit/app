package facades.electron

import scala.scalajs.js

@js.native
trait DialogModule extends js.Object {
	def showErrorBox(title: String, content: String): Unit = js.native
}
