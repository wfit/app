package models.composer

import models.{Toon, User}
import play.api.libs.json.{Format, Json}

case class RosterEntry (owner: User, rank: Int, toon: Toon)

object RosterEntry {
	implicit val format: Format[RosterEntry] = Json.format[RosterEntry]
}
