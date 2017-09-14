package gt

import gt.tools.{Http, View}
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.ext.PimpedNodeList
import play.api.libs.json._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.URIUtils

object Display {
	private var currentView: Option[View] = None
	private var tasks: Set[Future[_]] = Set.empty
	private var ready: Future[_] = Future.unit
	private var activeStyles: Set[html.Link] = Set.empty
	private var navigationInProgress: Boolean = false

	private var loading: Int = 0
	private var loadingStopTimer: js.timers.SetTimeoutHandle = null

	private def currentContainer = dom.document.querySelector("section#content")
	private def createContainer(): html.Element = {
		val container = dom.document.createElement("section").asInstanceOf[html.Element]
		container.id = "content"
		container
	}

	def beginLoading(): Unit = {
		loading += 1
		dom.document.body.classList.add("loading")
		cancelLoadStop()
	}

	private def cancelLoadStop(): Unit = {
		if (loadingStopTimer != null) {
			js.timers.clearTimeout(loadingStopTimer)
			loadingStopTimer = null
		}
	}

	def endLoading(): Unit = {
		loading = (loading - 1) max 0
		if (loading == 0) {
			cancelLoadStop()
			loadingStopTimer = js.timers.setTimeout(250) {
				if (loading == 0) {
					dom.document.body.classList.remove("loading")
				}
			}
		}
	}

	def init(): Unit = {
		loadSnippet(currentContainer.innerHTML, None)
	}

	private def loadMetadata(node: dom.NodeSelector): Unit = {
		tasks = Set.empty
		activeStyles = Set.empty
		Option(node.querySelector("script[type='application/gt-metadata']"))
			.map(_.textContent)
			.map(_.replace('+', ' '))
			.map(URIUtils.decodeURIComponent)
			.map(Json.parse)
			.map(_.as[JsObject])
			.foreach { metadata =>
				metadata.fields.foreach {
					case ("title", JsString(title)) => setTitle(title)
					case ("module", JsString(module)) => setModule(module)
					case ("module", JsNull) => setModule("none")
					case ("view", JsString(path)) => loadView(resolveView(path))
					case ("styles", JsArray(urls)) => loadStyles(urls.map(_.as[String]))
					case _ => // Ignore
				}
				ready = Future.sequence[Any, Set](tasks)
			}
	}

	private def resolveView(path: String): View = {
		path.split('.').foldLeft(js.Dynamic.global)((scope, key) => scope.selectDynamic(key)).asInstanceOf[View]
	}

	private def loadView(view: View): Unit = {
		currentView = Some(view)
	}

	private def unloadView(): Unit = {
		for (view <- currentView) view.unload()
		currentView = None
	}

	def navigate(url: String, method: String = "GET"): Unit = if (!navigationInProgress) {
		navigationInProgress = true
		beginLoading()
		val req = method match {
			case "POST" => Http.post(url)
			case "DELETE" => Http.delete(url)
			case "PUT" => Http.put(url)
			case _ => Http.get(url)
		}
		req.andThen { case _ => endLoading() }.foreach { res =>
			navigationInProgress = false
			handleResponse(res)
		}
	}

	def handleResponse(response: Http.Response): Unit = response match {
		case Http.Success(res) =>
			loadSnippet(res.text, Some(res.url))
		case Http.Redirect(location) =>
			if (response.status == 392) dom.document.location.href = location
			else navigate(location)
		case Http.Failure(code, err) =>
			Toast.error(err)
	}

	def loadSnippet(source: String, sourceUrl: Option[String] = None): Unit = {
		unloadView()
		val oldStyles = activeStyles

		val freshContainer = createContainer()
		freshContainer.innerHTML = source
		loadMetadata(freshContainer)

		for (script <- freshContainer.querySelectorAll("script")) {
			script.parentNode.removeChild(script)
		}

		for (url <- sourceUrl) {
			dom.window.history.replaceState(null, dom.document.title, url)
		}

		val view = currentView
		for (_ <- ready) {
			oldStyles.foreach(style => style.parentElement.removeChild(style))
			if (view == currentView) {
				val oldContainer = currentContainer
				oldContainer.parentNode.replaceChild(freshContainer, oldContainer)
				view.foreach(_.init())
				for (node <- Option(freshContainer.querySelector("[autofocus]"))) {
					node.asInstanceOf[html.Input].focus()
				}
			}
		}
	}

	private def loadStyles(urls: Seq[String]): Unit = {
		for (url <- urls) {
			val style = dom.document.createElement("link").asInstanceOf[html.Link]
			style.rel = "stylesheet"
			style.`type` = "text/css"
			style.href = url

			val promise = Promise[Unit]()
			tasks += promise.future

			style.addEventListener("load", (e: dom.Event) => promise.success(()))
			style.addEventListener("error", (e: dom.Event) => promise.success(()))

			activeStyles += style
			dom.document.head.appendChild(style)
		}
	}

	private def setTitle(title: String): Unit = {
		dom.document.title = title
		dom.document.querySelector("header h2").textContent = title
	}

	private def setModule(module: String): Unit = {
		for (node <- dom.document.querySelectorAll("body > nav .active")) {
			node.asInstanceOf[html.Element].classList.remove("active")
		}
		for (node <- Option(dom.document.querySelector(s"body > nav li[data-module='$module']"))) {
			node.classList.add("active")
		}
	}
}
