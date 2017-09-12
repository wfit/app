package facades.node

import scala.scalajs.js

@js.native
trait Hash extends Stream.Transform {
	def update(data: String): Unit = js.native
	def digest(encoding: String): String = js.native
}
