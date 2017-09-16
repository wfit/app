package gt.util

import gt.{GuildTools, Toast}
import org.scalajs.dom
import org.scalajs.dom.experimental.{RequestInit, Response => JSResponse, _}
import org.scalajs.dom.window.location
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.{JSON, URIUtils}
import scala.scalajs.js.typedarray.Uint8Array

object Http {
	val defaultHeaders = Map("Gt-Fetch" -> "1")
	val emptyHeaders = new Headers()

	private def extractHeaders(headers: Headers): Map[String, String] = {
		var extracted = Map.empty[String, String]
		for (key <- headers.asInstanceOf[js.Dynamic].keys().asInstanceOf[js.Iterator[String]].toIterator) {
			extracted += (key.toLowerCase -> headers.get(key).asInstanceOf[String])
		}
		extracted
	}

	class Response (raw: Option[JSResponse], val text: String) {
		def headers: Map[String, String] = raw.map(r => extractHeaders(r.headers)) getOrElse Map.empty
		def status: Int = raw.map(_.status) getOrElse 0
		def statusText: String = raw.map(_.statusText) getOrElse text
		def ok: Boolean = raw.exists(_.ok)
		def redirect: Boolean = 300 <= status && status < 400
		def url: String = raw.map(_.url).orNull

		lazy val contentType: String = headers.getOrElse("content-type", "?/?")
		lazy val json: JsValue = Json.parse(text)
		def as[T <: js.Any]: T = JSON.parse(text).asInstanceOf[T]
	}

	object Success {
		def unapply(arg: Response): Option[Response] = if (arg.ok) Some(arg) else None
	}

	object Redirect {
		def unapply(arg: Response): Option[String] = {
			if (arg.status == 392) Some(arg.headers.get("location").orNull)
			else None
		}
	}

	object Failure {
		private def errorText(response: Response): Option[String] = {
			response.headers.get("content-type").collect {
				case mime if mime startsWith "text/plain" => response.text
			}
		}

		def unapply(arg: Response): Option[(Int, String)] = {
			if (arg.ok || arg.redirect) None
			else Some(arg.status, errorText(arg) getOrElse arg.statusText)
		}
	}

	object EmptyBody

	trait HttpBody[-T] {
		def hasBody: Boolean
		def contentType: String
		def asString(body: T): String
	}

	object HttpBody {
		implicit object StringBody extends HttpBody[String] {
			val hasBody: Boolean = true
			val contentType: String = "text/plain"
			def asString(body: String): String = body
		}

		implicit object JsonBody extends HttpBody[JsValue] {
			val hasBody: Boolean = true
			val contentType: String = "application/json"
			def asString(body: JsValue): String = body.toString()
		}

		implicit object FormBody extends HttpBody[Map[String, String]] {
			val hasBody: Boolean = true
			val contentType: String = "application/x-www-form-urlencoded"
			def asString(body: Map[String, String]): String = {
				body.foldLeft("")((prev, item) => {
					val (key, value) = item
					s"$prev&${ URIUtils.encodeURIComponent(key) }=${ URIUtils.encodeURIComponent(value) }"
				}).drop(1)
			}
		}

		implicit object EmptyBodyBody extends HttpBody[EmptyBody.type] {
			val hasBody: Boolean = false
			def contentType: String = ???
			def asString(body: EmptyBody.type): String = ???
		}
	}

	trait ResponseMode {
		type R
		def apply(res: Future[JSResponse]): Future[R]
	}

	object BufferResponse extends ResponseMode {
		type R = Response
		def apply(res: Future[JSResponse]): Future[Response] = {
			res.flatMap { response =>
				// Handle response
				if (300 <= response.status && response.status < 400) {
					Future.successful(new Response(Some(response), null))
				} else {
					response.text().toFuture.map(text => new Response(Some(response), text))
				}
			}.recover {
				case err => new Response(None, err.getMessage)
			}
		}
	}

	object NavigationResponse extends ResponseMode {
		private def handleHttpHooks(response: dom.experimental.Response): Boolean = {
			// Reads a header from response
			def header(name: String): Option[String] = Option(response.headers.get(name).asInstanceOf[String])

			val stateHash = header("gt-statehash")
			val instance = header("gt-instance")
			val method = header("gt-method")

			if (stateHash.exists(_ != GuildTools.stateHash) && method.contains("GET")) {
				GuildTools.reload(response.url)
				false
			} else if (instance.exists(_ != GuildTools.instanceUUID)) {
				if (method.contains("GET")) {
					GuildTools.reload(response.url)
					false
				} else {
					Toast.serverUpdated()
					true
				}
			} else {
				true
			}
		}

		type R = Response
		def apply(res: Future[JSResponse]): Future[Response] = {
			BufferResponse(res.flatMap { response =>
				if (GuildTools.isWorker || handleHttpHooks(response)) res
				else Future.never
			})
		}
	}

	object StreamResponse extends ResponseMode {
		type R = ReadableStream[Uint8Array]
		def apply(res: Future[JSResponse]): Future[ReadableStream[Uint8Array]] = {
			res.filter(_.status == 200).map(_.body)
		}
	}

	private def fullUrl(url: String): String = {
		if (url contains ":") url
		else {
			require(url startsWith "/", "non-absolute URLs must be path-absolute")
			if (GuildTools.isWorker) s"${ location.origin }$url"
			else s"${ location.protocol }//${ location.host }$url"
		}
	}

	def fetch[B](url: String, method: HttpMethod, body: B = EmptyBody,
	             headers: Map[String, String] = Map.empty,
	             mode: ResponseMode = BufferResponse)
	            (implicit format: HttpBody[B]): Future[mode.R] = {
		val composedHeaders = new Headers()

		for ((n, v) <- defaultHeaders) composedHeaders.set(n, v)
		for ((n, v) <- headers) composedHeaders.set(n, v)
		if (format.hasBody) composedHeaders.set("Content-type", format.contentType)

		val settings = js.Dictionary(
			"method" -> method,
			"headers" -> composedHeaders,
			"body" -> (if (format.hasBody) format.asString(body) else js.undefined),
			"mode" -> RequestMode.`same-origin`,
			"credentials" -> RequestCredentials.include
		).asInstanceOf[RequestInit]

		mode(Fetch.fetch(fullUrl(url), settings).toFuture)
	}

	def get(url: String, headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.GET, EmptyBody, headers, BufferResponse)
	}

	def delete(url: String, headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.DELETE, EmptyBody, headers, BufferResponse)
	}

	def post[B: HttpBody](url: String, body: B = EmptyBody,
	                      headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.POST, body, headers, BufferResponse)
	}

	def put[B: HttpBody](url: String, body: B = EmptyBody,
	                     headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.PUT, body, headers, BufferResponse)
	}
}
