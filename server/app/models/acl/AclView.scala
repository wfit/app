package models.acl

import utils.UUID
import utils.SlickAPI._

class AclView (tag: Tag) extends Table[AclEntry](tag, "gt_acl_view") {
	def user = column[UUID]("user", O.PrimaryKey)
	def key = column[String]("key")
	def value = column[Int]("value")

	def * = (user, key, value) <> (AclEntry.tupled, AclEntry.unapply)
}

object AclView extends TableQuery(new AclView(_)) {
	def queryKey(key: String)(criterion: Rep[Int] => Rep[Boolean]): Query[Rep[UUID], UUID, Seq] = {
		AclView.filter(e => e.key === key && criterion(e.value)).map(_.user)
	}

	def granted(user: Rep[UUID], key: Rep[String]): Query[Rep[Int], Int, Seq] = {
		AclView.filter(e => e.user === user && e.key === key).map(_.value)
	}
}
