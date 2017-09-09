package gt.modules.login

import gt.GuildTools
import gt.tools.{CustomEvent, View}
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportTopLevel("views.login.form")
object Form extends View {
	private def form = $[html.Form]("#login form")
	private def id = $[html.Input]("#login input[name='id']")
	private def pass = $[html.Input]("#login input[name='pass']")
	private def error = $[html.Div]("#login .box.error")

	override def init(): Unit = {
		// Handle situation where user get unexpectedly disconnected
		if (GuildTools.isAuthenticated) {
			dom.document.location.reload()
		}

		localStorage("login.identifier") match {
			case Some(username) if username.trim.nonEmpty =>
				id.value = username.trim
				pass.focus()
			case None =>
				id.focus()
		}

		id.addEventListener("input", (_: dom.Event) => localStorage.update("login.identifier", id.value))
		form.addEventListener("submit", (_: dom.Event) => error.setAttribute("hidden", ""))

		form.addEventListener("error", (event: CustomEvent[String]) => {
			error.textContent = event.detail
			error.removeAttribute("hidden")
			pass.value = ""
			pass.focus()
		})
	}
}
