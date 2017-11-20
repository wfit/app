package db.acl

import db.api._
import models.UUID
import models.acl.AclMembership

class AclMemberships(tag: Tag) extends Table[AclMembership](tag, "gt_acl_memberships") {
	def user = column[UUID]("user")
	def group = column[UUID]("group")

	def * = (user, group) <> ((AclMembership.apply _).tupled, AclMembership.unapply)
}

object AclMemberships extends TableQuery(new AclMemberships(_))
