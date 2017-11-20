package db.acl

import models.UUID

case class AclEntry(user: UUID, key: String, value: Int)
