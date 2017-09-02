package models

import java.time.Instant
import utils.UUID
import utils.SlickAPI._

class Sessions (tag: Tag) extends Table[Session](tag, "gt_sessions") {
	def id = column[UUID]("id")
	def user = column[UUID]("user")
	def lastAccess = column[Instant]("last_access")

	def * = (id, user) <> (Session.tupled, Session.unapply)
}

object Sessions extends TableQuery(new Sessions(_)) {
	val findById = this.findBy(_.id)
}
