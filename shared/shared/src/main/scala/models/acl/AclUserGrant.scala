package models.acl

import utils.UUID

case class AclUserGrant(subject: UUID, key: UUID, value: Int, negate: Boolean, over: Boolean) extends AclGrant
