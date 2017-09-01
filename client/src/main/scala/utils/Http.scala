package utils

import gt.Toast
import org.scalajs.dom
import org.scalajs.dom.experimental.{RequestInit, Response => JSResponse, _}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.URIUtils

object Http {
	private var serverInstance: js.UndefOr[String] = js.undefined
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

	def fetch[B](url: String, method: HttpMethod, body: B = EmptyBody,
	             headers: Map[String, String] = Map.empty)
	            (implicit format: HttpBody[B]): Future[Response] = {
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

		Fetch.fetch(url, settings).toFuture.flatMap { response =>
			val instance = response.headers.get("gt-instance").asInstanceOf[String]
			if (serverInstance.isEmpty) serverInstance = instance
			else if (serverInstance.asInstanceOf[String] != instance) Toast.serverUpdated()

			if (300 <= response.status && response.status < 400) {
				Future.successful(new Response(Some(response), null))
			} else {
				response.text().toFuture.map(text => new Response(Some(response), text))
			}
		}.recover {
			case err => new Response(None, err.getMessage)
		}
	}

	def get(url: String, headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.GET, EmptyBody, headers)
	}

	def delete(url: String, headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.DELETE, EmptyBody, headers)
	}

	def post[B: HttpBody](url: String, body: B = EmptyBody,
	                      headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.POST, body, headers)
	}

	def put[B: HttpBody](url: String, body: B = EmptyBody,
	                     headers: Map[String, String] = Map.empty): Future[Response] = {
		fetch(url, HttpMethod.PUT, body, headers)
	}
}
