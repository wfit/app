package graphql

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

case class JsonError(json: JsValue) extends Exception(json.toString()) {
	def obj: JsObject = json.validate[JsObject] getOrElse Json.obj("error" -> json)
}

object JsonError {
	def apply(str: String): JsonError = JsonError(JsString(str))
}
