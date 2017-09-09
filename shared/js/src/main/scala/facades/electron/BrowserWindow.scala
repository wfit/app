package facades.electron

import facades.node.EventEmitter
import scala.scalajs.js

@js.native
trait BrowserWindow extends EventEmitter {
	def show(): Unit = js.native
	def hide(): Unit = js.native
	def close(): Unit = js.native
	def destroy(): Unit = js.native

	def minimize(): Unit = js.native
	def maximize(): Unit = js.native
	def unmaximize(): Unit = js.native
	def isMaximized(): Boolean = js.native

	def loadURL(url: String): Unit = js.native

	def webContents: WebContent
}
