package facades.electron

import facades.node.EventEmitter
import scala.scalajs.js

@js.native
trait Tray extends EventEmitter {
	def setToolTip(text: String): Unit = js.native
	def setContextMenu(menu: Menu): Unit = js.native
}
