package models.composer

import models.wow.{Class, Role}
import play.api.libs.json.{Format, Json}
import utils.UUID

case class GroupSlot (fragment: UUID, slot: UUID, tier: Int, toon: Option[UUID],
                      name: String, role: Role, cls: Class)

object GroupSlot {
	implicit val format: Format[GroupSlot] = Json.format[GroupSlot]
}
