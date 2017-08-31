package models

import utils.SlickAPI._

class Credentials (tag: Tag) extends Table[Credential](tag, "milbb_users") {
	def id = column[Int]("user_id")
	def name = column[String]("username")
	def name_clean = column[String]("username_clean")
	def mail = column[String]("user_email")
	def password = column[String]("user_password")

	def * = (id, name, mail, password) <> (Credential.tupled, Credential.unapply)
}

object Credentials extends TableQuery(new Credentials(_))