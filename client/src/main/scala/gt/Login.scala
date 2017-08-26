package gt

import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}
import utils.ViewBehavior

@JSExportAll
@JSExportTopLevel("gt.login")
object Login extends ViewBehavior {
	private def form = $[html.Form]("#login form")
	private def id = $[html.Input]("#login-id")
	private def pass = $[html.Input]("#login-pass")
	private def btn = $[html.Button]("#login-btn")

	def setup(): Unit = {
		localStorage("gt.username") match {
			case Some(username) if username.trim.nonEmpty =>
				id.value = username.trim
				pass.focus()
			case None =>
				id.focus()
		}

		id.addEventListener("input", (e: dom.Event) => {
			localStorage.update("gt.username", id.value)
		})

		form.addEventListener("submit", (e: dom.Event) => {
			println("submit")
			e.preventDefault()
		})
	}
}
