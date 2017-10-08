package gt

import facades.html5
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js

object Tooltip {
	private var currentAnchor: html5.Element = null
	private var currentTooltip: html5.Element = null

	private val moveListener: js.Function1[dom.MouseEvent, Unit] = move _
	private val leaveListener: js.Function1[dom.MouseEvent, Unit] = leave _

	private def Event(name: String, cancelable: Boolean = false): dom.Event = {
		js.Dynamic.newInstance(js.constructorOf[dom.Event])(name, js.Dynamic.literal(cancelable = cancelable)).asInstanceOf[dom.Event]
	}

	def setup(): Unit = {
		dom.document.addEventListener("mouseover", { event: dom.MouseEvent =>
			if (event.target.isInstanceOf[html.Element]) {
				val el = event.target.asInstanceOf[html5.Element]
				el.closest("[tooltip]") match {
					case null => // Ignore
					case anchor => show(event, anchor)
				}
			}
		}, true)
	}

	private def show(event: dom.MouseEvent, anchor: html5.Element): Unit = {
		event.stopPropagation()
		if (anchor != currentAnchor) {
			// Find tooltip node
			anchor.classList.add("tooltip-current-anchor")
			anchor.querySelector(".tooltip-current-anchor > .tooltip") match {
				case tooltip: html.Element =>
					if (tooltip.dispatchEvent(Event("show", cancelable = true))) {
						if (currentAnchor != null) hide()
						currentAnchor = anchor
						currentTooltip = tooltip
						anchor.addEventListener("mousemove", moveListener, useCapture = true)
						anchor.addEventListener("mouseleave", leaveListener, useCapture = true)
						anchor.addEventListener("mousedown", leaveListener, useCapture = true)
						tooltip.classList.add("visible")
						move(event)
					}

				case _ =>
					dom.console.warn("No tooltip found for tooltip-enabled node", anchor)
			}
		}
	}

	private def move(event: dom.MouseEvent): Unit = {
		currentTooltip.style.bottom = (dom.document.body.clientHeight - event.clientY + 10).toString + "px"
		currentTooltip.style.left = event.clientX + 10 + "px"
	}

	private def leave(event: dom.MouseEvent): Unit = hide()

	private def hide(): Unit = {
		currentAnchor.classList.remove("tooltip-current-anchor")
		currentAnchor.removeEventListener("mousemove", moveListener, useCapture = true)
		currentAnchor.removeEventListener("mouseleave", leaveListener, useCapture = true)
		currentAnchor.removeEventListener("mousedown", leaveListener, useCapture = true)
		currentAnchor = null

		currentTooltip.classList.remove("visible")
		currentTooltip.dispatchEvent(Event("close"))
		currentTooltip = null
	}
}
