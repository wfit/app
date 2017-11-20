package base

import utils.UserAcl

@FunctionalInterface
trait AclCriterion {
	def check(acl: UserAcl): Boolean

	final def | (other: AclCriterion): AclCriterion = (acl) => check(acl) || other.check(acl)
	final def & (other: AclCriterion): AclCriterion = (acl) => check(acl) && other.check(acl)
}

object AclCriterion {
	implicit def fromString(key: String): AclCriterion = SimpleAclCriterion(key, _ > 0)
	implicit def fromTuple(crit: (String, Int)): AclCriterion = SimpleAclCriterion(crit._1, _ > crit._2)
	implicit def fromTuple(crit: (String, Int, Int)): AclCriterion = SimpleAclCriterion(crit._1, _ > crit._2, crit._3)

	case class SimpleAclCriterion(key: String, predicate: Int => Boolean, default: Int = 0) extends AclCriterion {
		def check(acl: UserAcl): Boolean = predicate(acl.get(key, default))
	}
}
