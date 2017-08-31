package models

import utils.UUID

case class AclMembership(user: UUID, group: UUID)
