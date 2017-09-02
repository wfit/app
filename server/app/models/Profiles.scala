package models

import java.time.LocalDate
import utils.UUID
import utils.SlickAPI._

class Profiles (tag: Tag) extends Table[Profile](tag, "gt_profiles") {
	def user = column[UUID]("user", O.PrimaryKey)

	def name = column[Option[String]]("name")
	def nameVisibility = column[Int]("name_visibility")

	def birthday = column[Option[LocalDate]]("birthday")
	def birthdayVisibility = column[Int]("birthday_visibility")

	def location = column[Option[String]]("location")
	def locationVisibility = column[Int]("location_visibility")

	def btag = column[Option[String]]("btag")
	def btagVisibility = column[Int]("btag_visibility")

	def mail = column[Option[String]]("mail")
	def mailVisibility = column[Int]("mail_visibility")

	def phone = column[Option[String]]("phone")
	def phoneVisibility = column[Int]("phone_visibility")

	def * = (user,
		        name, nameVisibility,
		        birthday, birthdayVisibility,
		        location, locationVisibility,
		        btag, btagVisibility,
		        mail, mailVisibility,
		        phone, phoneVisibility) <> ((Profile.apply _).tupled, Profile.unapply)
}

object Profiles extends TableQuery(new Profiles(_))
