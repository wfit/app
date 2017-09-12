package facades.node

import scala.scalajs.js

@js.native
trait Stats extends js.Object {
	def isFile(): Boolean = js.native
	def isDirectory(): Boolean = js.native
}
