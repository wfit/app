package models

import models.acl.AclUserGrant
import utils.UUID
import utils.SlickAPI._

class AclUserGrants(tag: Tag) extends Table[AclUserGrant](tag, "gt_acl_user_grants") {
	def subject = column[UUID]("subject")
	def key = column[UUID]("key")
	def value = column[Int]("value")
	def negate = column[Boolean]("negate")
	def over = column[Boolean]("override")

	def * = (subject, key, value, negate, over) <> (AclUserGrant.tupled, AclUserGrant.unapply)
}

object AclUserGrants extends TableQuery(new AclUserGrants(_))
