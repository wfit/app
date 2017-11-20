package models

import play.api.libs.json.{Format, Json}
import scala.language.implicitConversions
import utils.PrimaryKey

case class User(uuid: UUID, name: String, group: Int)

object User {
	val guest = User(UUID.zero, "Guest", -1)

	implicit def extractUUID(user: User): UUID = user.uuid

	implicit val format: Format[User] = Json.format[User]
	implicit val key: PrimaryKey[User, UUID] = user => user.uuid
}
