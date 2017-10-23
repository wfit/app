package gt

import gt.util.{Http, View}
import gt.workers.{Worker, WorkerRef}
import org.scalajs.dom
import org.scalajs.dom.experimental.HttpMethod
import org.scalajs.dom.ext.PimpedNodeList
import org.scalajs.dom.html
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.URIUtils
import scala.scalajs.reflect.Reflect

/**
  * The display is the main component responsible for managing the global
  * view of the application. It renders fragments of document in the main
  * view port of the app and ensure that metadata and dependencies are
  * correctly fetched beforehand.
  *
  * It also manages navigation and history book-keeping.
  */
object Display {
	private var currentView: Option[ViewWorker] = None
	private var tasks: Set[Future[_]] = Set.empty
	private var ready: Future[_] = Future.unit
	private var navigationInProgress: Boolean = false

	private var loading: Int = 0
	private var loadingStopTimer: js.timers.SetTimeoutHandle = null

	private def currentContainer = dom.document.querySelector("section#content")
	private def createContainer(): html.Element = {
		val container = dom.document.createElement("section").asInstanceOf[html.Element]
		container.id = "content"
		container
	}

	/**
	  * Begins the load animation
	  */
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

	/**
	  * Stops the loading animation
	  */
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

	/**
	  * Initial display initialization.
	  * Initial snippet will be available in the default container from the
	  * Play view output.
	  */
	def init(): Unit = {
		loadSnippet(currentContainer.innerHTML, None)
	}

	/**
	  * Loads metadata from the given node.
	  */
	private def loadMetadata(node: dom.NodeSelector): Unit = {
		tasks = Set.empty
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
					case ("view", JsString(path)) => loadView(path)
					case _ => // Ignore
				}
				ready = Future.sequence[Any, Set](tasks)
			}
	}

	/**
	  * Loads the given view.
	  */
	private def loadView(path: String): Unit = {
		currentView = Some(ViewWorker(path))
	}

	/**
	  * Unlaods the current view, if any.
	  */
	private def unloadView(): Unit = {
		for (view <- currentView) view.unload()
		currentView = None
	}

	/**
	  * Triggers top-level navigation.
	  * The given URL will be fetched and displayed as the current view of the app.
	  *
	  * A call to this method will be silently ignored if a navigation is already
	  * in progress.
	  *
	  * @param url target URL
	  * @param method HTTP method to use (POST / DELETE / PUT / GET)
	  */
	def navigate(url: String, method: String = "GET"): Unit = if (!navigationInProgress) {
		navigationInProgress = true
		beginLoading()
		val req = method match {
			case "POST" => Http.fetch(url, HttpMethod.POST, mode = Http.NavigationResponse)
			case "DELETE" => Http.fetch(url, HttpMethod.DELETE, mode = Http.NavigationResponse)
			case "PUT" => Http.fetch(url, HttpMethod.PUT, mode = Http.NavigationResponse)
			case _ => Http.fetch(url, HttpMethod.GET, mode = Http.NavigationResponse)
		}
		req.andThen { case _ => endLoading() }.foreach { res =>
			navigationInProgress = false
			handleResponse(res)
		}
	}

	/**
	  * Handles the response from fetch initiated in [[navigate]].
	  * @param response the response object
	  */
	def handleResponse(response: Http.Response): Unit = response match {
		case Http.Success(res) =>
			loadSnippet(res.text, Some(res.url))
		case Http.Redirect(location) =>
			if (response.status == 392) dom.document.location.href = location
			else navigate(location)
		case Http.Failure(code, err) =>
			Toast.error(err)
	}

	/**
	  * Loads a code snippet as current view content.
	  *
	  * This includes:
	  * - Unloading the previous view
	  * - Creating a new container object
	  * - Injecting the HTML code inside the container
	  * - Reading and executing metadata associated with the view
	  * - Initializing the view controller object
	  *
	  * If the source URL is given, the history entry for the current
	  * page is also updated.
	  *
	  * @param source the source code of the view
	  * @param sourceUrl the source URL of the view, if available
	  */
	def loadSnippet(source: String, sourceUrl: Option[String] = None): Unit = {
		unloadView()

		val freshContainer = createContainer()
		freshContainer.innerHTML = source
		loadMetadata(freshContainer)

		for (script <- freshContainer.querySelectorAll("script[type='application/gt-metadata']")) {
			script.parentNode.removeChild(script)
		}

		for (url <- sourceUrl) {
			dom.window.history.replaceState(null, dom.document.title, url)
		}

		val view = currentView
		for (_ <- ready) {
			if (view == currentView) {
				val oldContainer = currentContainer
				oldContainer.parentNode.replaceChild(freshContainer, oldContainer)
				for (v <- view) v.init()
				for (node <- Option(freshContainer.querySelector("[autofocus]"))) {
					node.asInstanceOf[html.Input].focus()
				}
			}
		}
	}

	/**
	  * Sets the page title
	  *
	  * @param title the page title
	  */
	private def setTitle(title: String): Unit = {
		dom.document.title = title
		dom.document.querySelector("header h2").textContent = title
	}

	/**
	  * Sets the currently active module on the sidebar.
	  *
	  * @param module the module code
	  */
	private def setModule(module: String): Unit = {
		for (node <- dom.document.querySelectorAll("body > nav .active")) {
			node.asInstanceOf[html.Element].classList.remove("active")
		}
		for (node <- Option(dom.document.querySelector(s"body > nav li[data-module='$module']"))) {
			node.classList.add("active")
		}
	}

	case class ViewWorker(path: String) {
		private var instance = WorkerRef.NoWorker

		def init(): Unit = {
			instance = Worker.localDynamic(path)
		}

		def unload(): Unit = {
			instance.terminate()
			instance = WorkerRef.NoWorker
		}
	}
}
