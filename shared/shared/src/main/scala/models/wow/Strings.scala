package models.wow

object Strings {
	def raceName(id: Int): String = id match {
		case 1 => "Human"
		case 2 => "Orc"
		case 3 => "Dwarf"
		case 4 => "Night Elf"
		case 5 => "Undead"
		case 6 => "Tauren"
		case 7 => "Gnome"
		case 8 => "Troll"
		case 9 => "Goblin"
		case 10 => "Blood Elf"
		case 11 => "Draenei"
		case 22 => "Worgen"
		case 24 | 25 | 26 => "Pandaren"
		case _ => "Unknown"
	}
}
