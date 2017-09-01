package facades.electron

import scala.scalajs.js

@js.native
trait WebFrameModule extends js.Object {
	def clearCache(): Unit = js.native
}
