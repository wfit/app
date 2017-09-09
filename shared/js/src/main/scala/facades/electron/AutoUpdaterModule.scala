package facades.electron

import facades.node.EventEmitter
import scala.scalajs.js

@js.native
trait AutoUpdaterModule extends EventEmitter {
	def checkForUpdates(): Unit = js.native
}
