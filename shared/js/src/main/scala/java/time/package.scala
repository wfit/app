package java

import play.api.libs.json.{Format, JsResult, JsString, JsValue}

package object time {
	implicit object InstantFormat extends Format[Instant] {
		def writes(instant: Instant): JsValue = JsString(instant.toString)
		def reads(json: JsValue): JsResult[Instant] = json.validate[String].map(Instant.parse)
	}
}
