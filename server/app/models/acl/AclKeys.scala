package models.acl

import utils.UUID
import utils.SlickAPI._

class AclKeys (tag: Tag) extends Table[AclKey](tag, "gt_acl_keys") {
	def id = column[UUID]("id", O.PrimaryKey)
	def key = column[String]("key")
	def desc = column[String]("desc")

	def * = (id, key, desc) <> (AclKey.tupled, AclKey.unapply)
}

object AclKeys extends TableQuery(new AclKeys(_))
