package facades.node

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@JSGlobal
@js.native
class Buffer extends js.Object {
	def toString(encoding: String): String = js.native
}

@JSGlobal
@js.native
object Buffer extends js.Object {
	def from(arrayBuffer: js.typedarray.ArrayBuffer): Buffer = js.native
}
