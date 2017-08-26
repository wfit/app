package facades.html5

import facades.html5
import org.scalajs
import org.scalajs.dom
import scala.scalajs.js

@js.native
trait Element extends dom.Element {
	def closest(selectors: String): html5.Element = js.native
}
