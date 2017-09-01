package gt

import org.scalajs.dom
import org.scalajs.dom.html
import scala.concurrent.duration._
import scala.scalajs.js

object Toast {
	case class Action (label: String, icon: Option[String] = None, alternate: Boolean = false)(action: => Unit) {
		def build(removeToast: () => Unit): html.Button = {
			val btn = dom.document.createElement("button").asInstanceOf[html.Button]
			btn.textContent = label
			if (alternate) btn.classList.add("alternate")
			for (i <- icon) {
				val node = dom.document.createElement("i")
				node.textContent = i
				btn.insertBefore(node, btn.firstChild)
			}
			btn.addEventListener("click", (e: dom.Event) => {
				removeToast()
				action
			})
			btn
		}
	}

	private lazy val container = dom.document.getElementById("toasts")

	private def display(tpe: String, msg: String,
	                    actions: Seq[Action] = Seq.empty,
	                    timer: js.UndefOr[FiniteDuration] = js.undefined): Unit = {
		val toast = dom.document.createElement("div").asInstanceOf[html.Div]
		toast.classList.add("toast")

		val box = dom.document.createElement("div").asInstanceOf[html.Div]
		box.classList.add("box")
		box.classList.add(tpe)
		box.textContent = msg

		var visible = true
		def removeToast(): Unit = if (visible) {
			visible = false
			container.removeChild(toast)
		}

		if (actions.nonEmpty) {
			val actionsContainer = dom.document.createElement("div").asInstanceOf[html.Div]
			actionsContainer.classList.add("actions")
			for (action <- actions) actionsContainer.appendChild(action.build(removeToast _))
			box.appendChild(actionsContainer)
		}

		toast.appendChild(box)
		container.appendChild(toast)

		for (t <- timer) {
			js.timers.setTimeout(t)(removeToast())
		}
	}

	def error(msg: String): Unit = {
		display("error", msg, timer = 5.seconds)
	}

	private var serverUpdatedVisible = false
	def serverUpdated(): Unit = {
		if (dom.document.hidden) {
			GuildTools.reload()
		} else if (!serverUpdatedVisible) {
			val msg = "Le serveur a été mis à jour. Rechargez l'application pour éviter d'éventuels problèmes."
			val reload = Action("Recharger", Some("refresh"))(GuildTools.reload())
			display("update", msg, Seq(reload))
			serverUpdatedVisible = true
		}
	}
}
