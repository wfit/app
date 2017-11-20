package utils

case class UserAcl(grants: Map[String, Int]) {
	def get(key: String, default: Int = 0): Int = grants.getOrElse(key, default)
	def check(key: String, default: Int = 0, required: Int = 0): Boolean = get(key, default) > required
	def can(key: String, default: Boolean = false): Boolean = check(key, if (default) 1 else 0)
}

object UserAcl {
	val empty = UserAcl(Map.empty)
}
