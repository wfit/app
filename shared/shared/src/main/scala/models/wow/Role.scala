package models.wow

sealed abstract class Role(val key: String, val order: Int)

object Role {
	object Tank extends Role("TANK", 1)
	object Healing extends Role("HEALING", 2)
	object DPS extends Role("DPS", 3)
	object Unknown extends Role("UNKNOW", 9)

	implicit val ordering: Ordering[Role] = Ordering.by(role => role.order)
}
