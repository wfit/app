package protocol

import play.api.libs.json.{Format, JsValue}
import protocol.MessageSerializer.Json

object BasicJsonFormat {
	val JsValueFormat: Format[JsValue] = implicitly[Format[JsValue]]
}
