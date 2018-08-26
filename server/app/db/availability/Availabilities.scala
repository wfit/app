package db.availability

import db.api._
import java.time.LocalDateTime
import models.UUID
import models.availability.Availability

class Availabilities(tag: Tag) extends Table[Availability](tag, "gt_availabilities") {
  def user = column[UUID]("user")
  def from = column[LocalDateTime]("from")
  def to   = column[LocalDateTime]("to")

  def * = (user, from, to) <> ((Availability.apply _).tupled, Availability.unapply)
}

object Availabilities extends TableQuery(new Availabilities(_))
