package models.composer

import models.wow.{Class, Role}
import utils.UUID
import utils.SlickAPI._

class Slots (tag: Tag) extends Table[Slot](tag, "gt_composer_slots") {
	def id = column[UUID]("id", O.PrimaryKey)
	def fragment = column[UUID]("fragment")
	def row = column[Int]("row")
	def col = column[Option[Int]]("col")
	def toon = column[Option[UUID]]("toon")
	def name = column[String]("name")
	def role = column[Role]("role")
	def cls = column[Class]("class")

	private val tupleToSlot: ((UUID, UUID, Int, Option[Int], Option[UUID], String, Role, Class)) => Slot = {
		case (id, frag, row, col, toon, name, role, cls) => Slot(id, frag, row, col getOrElse -1, toon, name, role, cls)
	}

	private val slotToTuple: Slot => Option[(UUID, UUID, Int, Option[Int], Option[UUID], String, Role, Class)] = {
		case Slot(id, frag, row, col, toon, name, role, cls) => Some((id, frag, row, Option(col).filterNot(_ < 0), toon, name, role, cls))
	}

	def * = (id, fragment, row, col, toon, name, role, cls) <> (tupleToSlot, slotToTuple)
}

object Slots extends TableQuery(new Slots(_))
