package gt

import facades.html5
import org.scalajs.dom
import org.scalajs.dom.{html, NodeListOf}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.{CustomEvent, Http}
import org.scalajs.dom.ext._
import scala.util.{Failure, Success, Try}

object Interceptor {
	def setup(): Unit = {
		setupFormIntercept()
		setupLinkIntercept()
		setupHistoryIntercept()
	}

	private def setupFormIntercept(): Unit = {
		dom.document.addEventListener("submit", (e: dom.Event) => {
			e.preventDefault()

			val form = e.target.asInstanceOf[html.Form]
			if (form.classList.contains("in-flight")) {
				throw new IllegalStateException("Form is being submitted twice")
			}

			var params = Map.empty[String, JsValue]
			for (node <- form.querySelectorAll("input[name], select[name], textarea[name]"); input = node.asInstanceOf[html.Input]) {
				val value = input.getAttribute("typed") match {
					case "number" =>
						Try(input.value.toDouble) match {
							case Success(number) =>
								JsNumber(number)
							case Failure(e) =>
								Toast.error(s"Impossible d'interpréter '${input.value}' en tant que nombre.")
								throw e
						}
					case "boolean" =>
						JsBoolean(input.value match {
							case "" | "0" => false
							case _ => true
 						})
					case _ =>
						JsString(input.value)
				}
				params += (input.name -> value)
			}

			val errorDisplay = Option(form.getAttribute("error-display")).map(dom.document.getElementById)

			disableForm(form)
			for (node <- errorDisplay) {
				node.setAttribute("hidden", "")
				node.textContent = ""
			}

			Http.post(form.action, JsObject(params)).foreach { result =>
				enableForm(form)
				result match {
					case Http.Failure(code, err) =>
						form.dispatchEvent(CustomEvent("error", err))
						if (form.hasAttribute("error-toast")) {
							Toast.error(err)
						}
						for (node <- errorDisplay) {
							node.removeAttribute("hidden")
							node.textContent = err
						}
					case other =>
						Display.handleResponse(other)
				}
			}
		})
	}

	private def disableForm(form: html.Form): Unit = {
		form.classList.add("in-flight")
		for (node <- form.querySelectorAll("input, select, textarea, button:not([disabled]");
		     input = node.asInstanceOf[html.Input]) {
			input.disabled = true
			input.setAttribute("in-flight-disabled", "1")
		}
	}

	private def enableForm(form: html.Form): Unit = {
		form.classList.remove("in-flight")
		for (node <- form.querySelectorAll("input[in-flight-disabled], select[in-flight-disabled], textarea[in-flight-disabled], button[in-flight-disabled]");
		     input = node.asInstanceOf[html.Input]) {
			input.disabled = false
			input.removeAttribute("in-flight-disabled")
		}
	}

	private def setupLinkIntercept(): Unit = {
		dom.document.addEventListener("click", (e: dom.Event) => {
			Option(e.target.asInstanceOf[html5.HTMLElement].closest("a[href]")) match {
				case Some(link) => linkClicked(e, link.asInstanceOf[html.Anchor])
				case None => // ignore
			}
		})
	}

	private def linkClicked(event: dom.Event, link: html.Anchor): Unit = {
		Option(link.getAttribute("href")) match {
			case Some("") =>
				event.preventDefault()
			case Some(href) if href startsWith "/" =>
				event.preventDefault()
				Display.navigate(href, Option(link.getAttribute("method")).getOrElse("get"))
			case _ =>
			// ignore
		}
	}

	private def setupHistoryIntercept(): Unit = {
		dom.window.addEventListener("popstate", (e: dom.PopStateEvent) => {
			Display.navigate(dom.document.location.pathname)
		})
	}
}
