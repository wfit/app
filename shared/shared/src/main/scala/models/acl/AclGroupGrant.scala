package models.acl

import models.UUID

case class AclGroupGrant(subject: UUID, key: UUID, value: Int, negate: Boolean) extends AclGrant
