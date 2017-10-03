package models.composer

import models.wow.{Class, Role}
import utils.UUID
import utils.SlickAPI._

class GroupSlots (tag: Tag) extends Table[GroupSlot](tag, "gt_composer_group_slots") {
	def fragment = column[UUID]("fragment")
	def slot = column[UUID]("slot", O.PrimaryKey)
	def tier = column[Int]("tier")
	def toon = column[Option[UUID]]("toon", O.Unique)
	def name = column[String]("name")
	def role = column[Role]("role")
	def cls = column[Class]("class")

	def * = (fragment, slot, tier, toon, name, role, cls) <> ((GroupSlot.apply _).tupled, GroupSlot.unapply)
}

object GroupSlots extends TableQuery(new GroupSlots(_))
