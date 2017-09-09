package utils

import java.util.{UUID => JUUID}
import play.api.libs.json.{Format, JsResult, JsString, JsValue}

case class UUID (override val toString: String) extends AnyVal

object UUID {
	final val zero = UUID("00000000-0000-0000-0000-000000000000")
	def random: UUID = UUID(JUUID.randomUUID().toString)

	implicit object JsonFormat extends Format[UUID] {
		def reads(json: JsValue): JsResult[UUID] = json.validate[String].map(UUID.apply)
		def writes(o: UUID): JsValue = JsString(o.toString)
	}
}
