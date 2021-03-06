package db.acl

import db.api._
import models.UUID
import models.acl.AclGroupGrant

class AclGroupGrants(tag: Tag) extends Table[AclGroupGrant](tag, "gt_acl_group_grants") {
	def subject = column[UUID]("subject", O.PrimaryKey)
	def key = column[UUID]("key", O.PrimaryKey)
	def value = column[Int]("value")
	def negate = column[Boolean]("negate")

	def * = (subject, key, value, negate) <> ((AclGroupGrant.apply _).tupled, AclGroupGrant.unapply)
}

object AclGroupGrants extends TableQuery(new AclGroupGrants(_))
