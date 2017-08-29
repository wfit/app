package models

import scala.language.implicitConversions
import utils.UUID

case class User (uuid: UUID, name: String, group: Int,
                 mail: Option[String], tel: Option[String], btag: Option[String])

object User {
	val guest = User(UUID.zero, "Guest", -1, None, None, None)
	implicit def extractUUID(user: User): UUID = user.uuid
}
