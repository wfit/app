package facades.electron

import facades.node.EventEmitter
import scala.scalajs.js

@js.native
trait WebContent extends EventEmitter {
	def getUserAgent(): String = js.native
	def setUserAgent(ua: String): Unit = js.native
	def openDevTools(options: js.Object): Unit = js.native
	def reload(): Unit = js.native
	def executeJavaScript(code: String, gesture: Boolean = false): Unit = js.native
}
