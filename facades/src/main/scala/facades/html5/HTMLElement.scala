package facades.html5

import facades.html5
import org.scalajs.dom.html
import scala.scalajs.js

@js.native
trait HTMLElement extends html.Element with html5.Element {
	val dataset: Dataset = js.native
}
