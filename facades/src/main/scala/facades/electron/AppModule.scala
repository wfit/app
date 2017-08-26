package facades.electron

import facades.node.EventEmitter
import scala.scalajs.js

@js.native
trait AppModule extends EventEmitter {
	def quit(): Unit = js.native
	def releaseSingleInstance(): Unit = js.native
	def makeSingleInstance(cb: js.Function2[String, String, Unit]): Boolean = js.native
}
