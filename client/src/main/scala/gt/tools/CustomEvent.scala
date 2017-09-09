package gt.tools

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global, literal, newInstance}

@js.native
trait CustomEvent[T] extends dom.CustomEvent {
	override def detail: T = js.native
}

object CustomEvent {
	def apply[T](name: String, detail: js.UndefOr[T] = js.undefined,
	             bubbles: Boolean = false, cancelable: Boolean = false,
	             composed: Boolean = false): CustomEvent[T] = {
		newInstance(global.CustomEvent)(name, literal(
			detail = detail.asInstanceOf[js.Any],
			bubbles = bubbles,
			cancelable = cancelable,
			composed = composed
		)).asInstanceOf[CustomEvent[T]]
	}
}
