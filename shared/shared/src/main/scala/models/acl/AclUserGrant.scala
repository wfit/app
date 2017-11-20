package models.acl

import models.UUID

case class AclUserGrant(subject: UUID, key: UUID, value: Int, negate: Boolean, over: Boolean) extends AclGrant
