package models.composer

import models.wow.{Class, Role}
import play.api.libs.json.{Format, Json}
import utils.UUID

case class Slot (id: UUID, fragment: UUID, row: Int, col: Int,
                 toon: Option[UUID], name: String, role: Role, cls: Class)

object Slot {
	implicit val format: Format[Slot] = Json.format[Slot]
}
