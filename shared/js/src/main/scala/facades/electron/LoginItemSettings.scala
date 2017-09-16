package facades.electron

import scala.scalajs.js

@js.native
trait LoginItemSettings extends js.Object {
	val openAtLogin: Boolean = js.native
	val args: js.Array[String] = js.native
}

object LoginItemSettings {
	def apply(openAtLogin: Boolean  = false, args: js.Array[String] = js.Array()): LoginItemSettings =
		js.Dynamic.literal(openAtLogin = openAtLogin, args = args).asInstanceOf[LoginItemSettings]
}
