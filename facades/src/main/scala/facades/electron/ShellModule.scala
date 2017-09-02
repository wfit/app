package facades.electron

import scala.scalajs.js

@js.native
trait ShellModule extends js.Object {
	def openExternal(url: String): Unit = js.native
}
