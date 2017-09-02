package models.acl

import utils.UUID

trait AclGrant {
	val subject: UUID
	val key: UUID
	val value: Int
	val negate: Boolean
}
