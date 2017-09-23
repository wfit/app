package models.acl

import utils.UUID

case class AclEntry (user: UUID, key: String, value: Int)
