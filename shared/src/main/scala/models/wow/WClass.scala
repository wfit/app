package models.wow

sealed abstract class WClass (val id: Int)

object WClass {
	abstract class Dummy(id: Int) extends WClass(id)

	case object Undefined extends Dummy(0)
	case class Unknown(unknownId: Int) extends Dummy(unknownId)

	case object Warrior extends WClass(1)
	case object Paladin extends WClass(2)
	case object Hunter extends WClass(3)
	case object Rogue extends WClass(4)
	case object Priest extends WClass(5)
	case object DeathKnight extends WClass(6)
	case object Shaman extends WClass(7)
	case object Mage extends WClass(8)
	case object Warlock extends WClass(9)
	case object Monk extends WClass(10)
	case object Druid extends WClass(11)
	case object DemonHunter extends WClass(12)

	def fromId(id: Int): WClass = id match {
		case 0 => Undefined
		case 1 => Warrior
		case 2 => Paladin
		case 3 => Hunter
		case 4 => Rogue
		case 5 => Priest
		case 6 => DeathKnight
		case 7 => Shaman
		case 8 => Mage
		case 9 => Warlock
		case 10 => Monk
		case 11 => Druid
		case 12 => DemonHunter
		case other => Unknown(id)
	}
}
