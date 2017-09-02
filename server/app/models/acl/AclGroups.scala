package models.acl

import utils.UUID
import utils.SlickAPI._

class AclGroups (tag: Tag) extends Table[AclGroup](tag, "gt_acl_groups") {
	def uuid = column[UUID]("id")
	def title = column[String]("title")
	def forumGroup = column[Option[Int]]("forum_group")

	def * = (uuid, title, forumGroup) <> (AclGroup.tupled, AclGroup.unapply)
}

object AclGroups extends TableQuery(new AclGroups(_))
