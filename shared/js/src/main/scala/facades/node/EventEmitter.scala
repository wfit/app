package facades.node

import scala.scalajs.js

@js.native
trait EventEmitter extends js.Object {
	def on(eventName: String, listener: js.Function): this.type = js.native
	def once(eventName: String, listener: js.Function): this.type = js.native
	def removeAllListeners(event: js.UndefOr[String] = js.undefined): this.type = js.native
}
