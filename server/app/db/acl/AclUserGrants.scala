package db.acl

import db.api._
import models.UUID
import models.acl.AclUserGrant

class AclUserGrants(tag: Tag) extends Table[AclUserGrant](tag, "gt_acl_user_grants") {
	def subject = column[UUID]("subject")
	def key = column[UUID]("key")
	def value = column[Int]("value")
	def negate = column[Boolean]("negate")
	def over = column[Boolean]("override")

	def * = (subject, key, value, negate, over) <> ((AclUserGrant.apply _).tupled, AclUserGrant.unapply)
}

object AclUserGrants extends TableQuery(new AclUserGrants(_))
