package gt

import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs
import scala.concurrent.duration._
import scala.scalajs.js

object Toast {
	private lazy val container = dom.document.getElementById("toasts")

	private def display(tpe: String, msg: String, timer: FiniteDuration): Unit = {
		val toast = dom.document.createElement("div").asInstanceOf[html.Div]
		toast.classList.add("toast")

		val box = dom.document.createElement("div").asInstanceOf[html.Div]
		box.classList.add("box")
		box.classList.add(tpe)
		box.textContent = msg

		toast.appendChild(box)
		container.appendChild(toast)

		js.timers.setTimeout(timer) {
			container.removeChild(toast)
		}
	}

	def error(msg: String): Unit = {
		display("error", msg, 5.seconds)
	}
}
