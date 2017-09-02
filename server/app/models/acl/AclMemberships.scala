package models.acl

import utils.UUID
import utils.SlickAPI._

class AclMemberships(tag: Tag) extends Table[AclMembership](tag, "gt_acl_memberships") {
	def user = column[UUID]("user")
	def group = column[UUID]("group")

	def * = (user, group) <> (AclMembership.tupled, AclMembership.unapply)
}

object AclMemberships extends TableQuery(new AclMemberships(_))
