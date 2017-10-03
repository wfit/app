package models.wow

import play.api.libs.json._

sealed abstract class Role(val key: String, val order: Int)

object Role {
	object Tank extends Role("TANK", 1)
	object Healing extends Role("HEALING", 2)
	object DPS extends Role("DPS", 3)
	object Unknown extends Role("UNKNOWN", 9)

	def fromString(key: String): Role = key match {
		case "TANK" => Tank
		case "HEALING" => Healing
		case "DPS" => DPS
		case "UNKNOWN" => Unknown
		case other => throw new NoSuchElementException(other)
	}

	implicit val ordering: Ordering[Role] = Ordering.by(role => role.order)

	implicit object JsonFormat extends Format[Role] {
		def writes(role: Role): JsValue = JsString(role.key)
		def reads(json: JsValue): JsResult[Role] = json.validate[String].map(fromString)
	}
}
