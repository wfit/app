package db

import db.api._
import java.time.Instant
import models.{Session, UUID}

class Sessions (tag: Tag) extends Table[Session](tag, "gt_sessions") {
	def id = column[UUID]("id")
	def user = column[UUID]("user")
	def lastAccess = column[Instant]("last_access")

	def * = (id, user) <> (Session.tupled, Session.unapply)
}

object Sessions extends TableQuery(new Sessions(_)) {
	val findById = this.findBy(_.id)
}
