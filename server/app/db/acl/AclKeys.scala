package db.acl

import db.api._
import models.UUID
import models.acl.AclKey

class AclKeys(tag: Tag) extends Table[AclKey](tag, "gt_acl_keys") {
	def id = column[UUID]("id", O.PrimaryKey)
	def key = column[String]("key")
	def desc = column[String]("desc")

	def * = (id, key, desc) <> ((AclKey.apply _).tupled, AclKey.unapply)
}

object AclKeys extends TableQuery(new AclKeys(_))
