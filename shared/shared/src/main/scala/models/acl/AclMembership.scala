package models.acl

import models.UUID

case class AclMembership(user: UUID, group: UUID)
