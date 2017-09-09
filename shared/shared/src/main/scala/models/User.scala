package models

import scala.language.implicitConversions
import utils.UUID

case class User (uuid: UUID, name: String, group: Int)

object User {
	val guest = User(UUID.zero, "Guest", -1)
	implicit def extractUUID(user: User): UUID = user.uuid
}
