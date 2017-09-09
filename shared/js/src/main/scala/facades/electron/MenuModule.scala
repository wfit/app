package facades.electron

import scala.scalajs.js

@js.native
trait MenuModule extends js.Object {
	def buildFromTemplate(template: js.Array[js.Dynamic]): Menu = js.native
}
