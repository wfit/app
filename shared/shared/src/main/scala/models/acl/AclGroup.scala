package models.acl

import models.UUID

case class AclGroup(uuid: UUID, title: String, forumGroup: Option[Int])
