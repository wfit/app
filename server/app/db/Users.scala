package db

import db.api._
import models.{UUID, User}

class Users (tag: Tag) extends Table[User](tag, "gt_users_view") {
	def uuid = column[UUID]("uuid")
	def fid = column[Int]("fid")
	def name = column[String]("username")
	def group = column[Int]("group_id")

	def * = (uuid, name, group) <> ((User.apply _).tupled, User.unapply)
}

object Users extends TableQuery(new Users(_))
