package models

import java.time.Instant
import models.wow.{Class, Spec}
import utils.UUID
import utils.SlickAPI._

class Toons (tag: Tag) extends Table[Toon](tag, "gt_toons") {
	def uuid = column[UUID]("uuid")
	def name = column[String]("name")
	def realm = column[String]("realm")

	def owner = column[UUID]("owner")
	def main = column[Boolean]("main")
	def active = column[Boolean]("active")

	def cls = column[Class]("class")
	def spec = column[Spec]("spec")
	def race = column[Int]("race")
	def gender = column[Int]("gender")
	def level = column[Int]("level")

	def thumbnail = column[Option[String]]("thumbnail")
	def ilvl = column[Int]("ilvl")

	def lastUpdate = column[Instant]("last_update")
	def invalid = column[Boolean]("invalid")
	def failures = column[Int]("failures")

	def * = (uuid, name, realm, owner, main, active, cls, spec, race, gender, level, thumbnail, ilvl, lastUpdate, invalid, failures) <> ((Toon.apply _).tupled, Toon.unapply)
}

object Toons extends TableQuery(new Toons(_))
