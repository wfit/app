package gt

import facades.html5
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js

/**
  * Manages the app-level tooltip feature
  */
object Tooltip {
	/** Current element spawning the tooltip */
	private var currentAnchor: html5.Element = null

	/** Current tooltip node */
	private var currentTooltip: html5.Element = null

	/** The immediate parent of the tooltip node */
	private var currentTooltipParent: dom.Node = null

	/** The sibling node of the tooltip */
	private var currentTooltipNextNode: dom.Node = null

	/** The move listener */
	private val moveListener: js.Function1[dom.MouseEvent, Unit] = move _

	/** The leave listener */
	private val leaveListener: js.Function1[dom.MouseEvent, Unit] = leave _

	/** Simple custom event constructor */
	private def Event(name: String, cancelable: Boolean = false): dom.Event = {
		// This is quite complicated because Scala.js DOM does not support custom event constructor
		js.Dynamic.newInstance(js.constructorOf[dom.Event])(name, js.Dynamic.literal(cancelable = cancelable)).asInstanceOf[dom.Event]
	}

	/** Setups tooltip handling. Binds the mouseover event at the document level. */
	def setup(): Unit = {
		dom.document.addEventListener("mouseover", { event: dom.MouseEvent =>
			if (event.target.isInstanceOf[html.Element]) {
				// Search for a tooltip-enabled node
				val el = event.target.asInstanceOf[html5.Element]
				el.closest("[tooltip]") match {
					case null => // Ignore
					case anchor => show(event, anchor)
				}
			}
		}, true)

		dom.document.addEventListener("mousedown", { event: dom.MouseEvent =>
			if (currentTooltip != null) hide()
		}, true)
	}

	/** Shows the tooltip */
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
						currentTooltipParent = tooltip.parentNode
						currentTooltipNextNode = tooltip.nextSibling
						dom.document.body.appendChild(tooltip)
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

	/** Tooltip offset from mouse position */
	private final val offset = 10

	/** Handles mouse move events and tooltip placement */
	private def move(event: dom.MouseEvent): Unit = {
		// Window size
		val windowWidth = dom.document.body.clientWidth
		val windowHeight = dom.document.body.clientHeight

		// Default position
		val left = event.clientX + offset
		val bottom = windowHeight - event.clientY + offset

		// Tooltip size
		val width = currentTooltip.offsetWidth
		val height = currentTooltip.offsetHeight

		// X-Axis
		if (left + width < windowWidth - offset) {
			currentTooltip.style.left = left + "px"
			currentTooltip.style.right = "auto"
		} else {
			currentTooltip.style.left = "auto"
			currentTooltip.style.right = (windowWidth - left + offset * 2) + "px"
		}

		// Y-Axis
		if (bottom + height < windowHeight - 60) {
			currentTooltip.style.bottom = bottom + "px"
			currentTooltip.style.top = "auto"
		} else {
			currentTooltip.style.bottom = "auto"
			currentTooltip.style.top = (windowHeight - bottom + offset * 2) + "px"
		}
	}

	/** Leaving the anchor element */
	private def leave(event: dom.MouseEvent): Unit = hide()

	/** Hides the tooltip and unbind events */
	private def hide(): Unit = {
		currentAnchor.classList.remove("tooltip-current-anchor")
		currentAnchor.removeEventListener("mousemove", moveListener, useCapture = true)
		currentAnchor.removeEventListener("mouseleave", leaveListener, useCapture = true)
		currentAnchor.removeEventListener("mousedown", leaveListener, useCapture = true)
		currentAnchor = null

		currentTooltipParent.insertBefore(currentTooltip, currentTooltipNextNode)
		currentTooltip.classList.remove("visible")
		currentTooltip.dispatchEvent(Event("close"))
		currentTooltip = null
		currentTooltipParent = null
		currentTooltipNextNode = null
	}
}
