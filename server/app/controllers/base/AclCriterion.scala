package controllers.base

import utils.UserAcl

@FunctionalInterface
trait AclCriterion {
	def check(acl: UserAcl, req: UserRequest[_]): Boolean

	final def | (other: AclCriterion): AclCriterion = (acl, req) => check(acl, req) || other.check(acl, req)
}

object AclCriterion {
	implicit def fromString(key: String): AclCriterion = SimpleAclCriterion(key, _ > 0)
	implicit def fromTuple(crit: (String, Int)): AclCriterion = SimpleAclCriterion(crit._1, _ > crit._2)
	implicit def fromTuple(crit: (String, Int, Int)): AclCriterion = SimpleAclCriterion(crit._1, _ > crit._2, crit._3)
	implicit def fromReqFn(fn: UserRequest[_] => Boolean): AclCriterion = (acl, req) => fn(req)

	case class SimpleAclCriterion (key: String, pred: Int => Boolean, default: Int = 0) extends AclCriterion {
		def check(acl: UserAcl, req: UserRequest[_]): Boolean = pred(acl.get(key, default))
	}
}
