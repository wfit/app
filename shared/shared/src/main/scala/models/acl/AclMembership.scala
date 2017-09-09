package models.acl

import utils.UUID

case class AclMembership(user: UUID, group: UUID)
