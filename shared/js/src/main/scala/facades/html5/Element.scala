package facades.html5

import facades.html5
import org.scalajs.dom.html
import scala.scalajs.js

@js.native
trait Element extends html.Element {
	def closest(selectors: String): html5.Element = js.native
}
