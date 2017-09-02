package models.acl

import utils.UUID

case class AclGroupGrant(subject: UUID, key: UUID, value: Int, negate: Boolean) extends AclGrant
