package models.acl

import utils.UUID

case class AclGroup (uuid: UUID, title: String, forumGroup: Option[Int])
