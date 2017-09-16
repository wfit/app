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
	private var currentView: Option[DelayedViewInit] = None
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

	/**
	  * Resolves the view to load based on its fully qualified class name.
	  *
	  * First a Scala `object` is searched and used as a view. If no such object are
	  * found or the object is not an instance of [[View]], then a class with the
	  * given fqcn is looked for instead.
	  *
	  * If no matching view can be found a [[ClassNotFoundException]] is thrown.
	  *
	  * @param path the view fqcn
	  */
	private def resolveView(path: String): DelayedViewInit = {
		val objView = (Reflect.lookupLoadableModuleClass(path) orElse
		               Reflect.lookupLoadableModuleClass(path + "$")).flatMap { loadable =>
			// Ensure we did not match the companion object of a class-based view
			loadable.loadModule() match {
				case view: View => Some(new DelayedViewInit(view))
				case _ => None
			}
		}
		def clsView = Reflect.lookupInstantiatableClass(path).map { instantiatable =>
			new DelayedViewInit(instantiatable.newInstance().asInstanceOf[View])
		}
		objView orElse clsView getOrElse (throw new ClassNotFoundException(s"Unable to load view: $path"))
	}

	/**
	  * Loads the given view.
	  *
	  * This code is mostly legacy since the view initialization will
	  * be delayed until every additional dependencies are available.
	  *
	  * The view is given as a [[DelayedViewInit]] object to prevent
	  * early initialization of class-based views.
	  *
	  * @see [[loadSnippet]]
	  * @param view the view to load
	  */
	private def loadView(view: DelayedViewInit): Unit = {
		currentView = Some(view)
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

	/**
	  * Loads a view-specific set of stylesheets.
	  *
	  * Every stylesheets to be loaded will be associated with a future that will
	  * be resolved once the spreadsheet is loaded. Each of these future is stored
	  * in the tasks array that will be waited on before displaying the page to
	  * the user.
	  *
	  * @param urls the set of stylesheets
	  */
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

	/**
	  * An utility class that ensure that a view instance is not created before the
	  * invocation of its init method ought to be called.
	  *
	  * This is used to allow class-based view in addition to object-based view that
	  * are guaranteed to be instantiated just before their init method is called and
	  * can thus use the constructor body in place of overriding the init method while
	  * still being executing with the full and final DOM tree available.
	  *
	  * @param provider the view provider
	  */
	class DelayedViewInit(provider: => View) {
		private var instance: Option[View] = None

		def init(): Unit = {
			require(instance.isEmpty, "DelayedViewInit: double initialization")
			val view = provider
			view.init()
			instance = Some(view)
		}

		def unload(): Unit = instance.foreach(_.unload())
	}
}
