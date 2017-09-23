package models.wow

import models.wow.Artifact._
import models.wow.Role.{DPS, Healing, Tank}
import play.api.libs.json.{Format, JsNumber, JsResult, JsValue}

sealed abstract class Spec(val id: Int, val cls: Class, val name: String, val role: Role, val icon: String) {
	def this(id: Int, name: String, role: Role, icon: String)(implicit cls: Class) = this(id, cls, name, role, icon)

	val artifact: Artifact
}

object Spec {
	private lazy val index = Class.list.flatMap(_.specs).map(s => (s.id, s)).toMap
	def fromId(id: Int): Spec = index.getOrElse(id, Dummy)

	abstract class ClassSpecs(cls: Class) {
		protected implicit val implCls: Class = cls
	}

	// Dummy
	object Dummy extends Spec(0, Class.Unknown, "Unknown", Role.Unknown, "") {
		val artifact = Atiesh
	}

	// Warrior
	object Warrior extends ClassSpecs(Class.Warrior) {
		object Arms extends Spec(71, "Arms", DPS, "ability_warrior_savageblow") {
			val artifact = Stromkar
		}

		object Fury extends Spec(72, "Fury", DPS, "ability_warrior_innerrage") {
			val artifact = WarswordsOfTheValarjar
		}

		object Protection extends Spec(73, "Protection", Tank, "ability_warrior_defensivestance") {
			val artifact = ScaleOfTheEarthWarder
		}
	}

	// Paladin
	object Paladin extends ClassSpecs(Class.Paladin) {
		object Holy extends Spec(65, "Holy", Healing, "spell_holy_holybolt") {
			val artifact = TheSilverHand
		}

		object Protection extends Spec(66, "Protection", Tank, "ability_paladin_shieldofthetemplar") {
			val artifact = Truthguard
		}

		object Retribution extends Spec(70, "Retribution", DPS, "spell_holy_auraoflight") {
			val artifact = Ashbringer
		}
	}

	// Hunter
	object Hunter extends ClassSpecs(Class.Hunter) {
		object BeastMastery extends Spec(253, "Beast Mastery", DPS, "ability_hunter_bestialdiscipline") {
			val artifact = Titanstrike
		}

		object Marksmanship extends Spec(254, "Marksmanship", DPS, "ability_hunter_focusedaim") {
			val artifact = Thasdorah
		}

		object Survival extends Spec(255, "Survival", DPS, "ability_hunter_camouflage") {
			val artifact = Talonclaw
		}
	}

	// Rogue
	object Rogue extends ClassSpecs(Class.Rogue) {
		object Assassination extends Spec(259, "Assassination", DPS, "ability_rogue_deadlybrew") {
			val artifact = TheKingslayers
		}

		object Outlaw extends Spec(260, "Outlaw", DPS, "inv_sword_30") {
			val artifact = TheDreadblades
		}

		object Subtlety extends Spec(261, "Subtlety", DPS, "ability_stealth") {
			val artifact = FangsOfTheDevourer
		}
	}

	// Priest
	object Priest extends ClassSpecs(Class.Priest) {
		object Discipline extends Spec(256, "Discipline", Healing, "spell_holy_powerwordshield") {
			val artifact = LightsWrath
		}

		object Holy extends Spec(257, "Holy", Healing, "spell_holy_guardianspirit") {
			val artifact = Tuure
		}

		object Shadow extends Spec(258, "Shadow", DPS, "spell_shadow_shadowwordpain") {
			val artifact = Xalatath
		}
	}

	// Death Knight
	object DeathKnight extends ClassSpecs(Class.DeathKnight) {
		object Blood extends Spec(250, "Blood", Tank, "spell_deathknight_bloodpresence") {
			val artifact = MawOfTheDamned
		}

		object Frost extends Spec(251, "Frost", DPS, "spell_deathknight_frostpresence") {
			val artifact = BladesOfTheFallenPrince
		}

		object Unholy extends Spec(252, "Unholy", DPS, "spell_deathknight_unholypresence") {
			val artifact = Apocalypse
		}
	}

	// Shaman
	object Shaman extends ClassSpecs(Class.Shaman) {
		object Elemental extends Spec(262, "Elemental", DPS, "spell_nature_lightning") {
			val artifact = TheFistOfRaden
		}

		object Enhancement extends Spec(263, "Enhancement", DPS, "spell_shaman_improvedstormstrike") {
			val artifact = Doomhammer
		}

		object Restoration extends Spec(264, "Restoration", Healing, "spell_nature_magicimmunity") {
			val artifact = Sharasdal
		}
	}

	// Mage
	object Mage extends ClassSpecs(Class.Mage) {
		object Arcane extends Spec(62, "Arcane", DPS, "spell_holy_magicalsentry") {
			val artifact = Aluneth
		}

		object Fire extends Spec(63, "Fire", DPS, "spell_fire_firebolt02") {
			val artifact = Felomelorn
		}

		object Frost extends Spec(64, "Frost", DPS, "spell_frost_frostbolt02") {
			val artifact = Ebonchill
		}
	}

	// Warlock
	object Warlock extends ClassSpecs(Class.Warlock) {
		object Affliction extends Spec(265, "Affliction", DPS, "spell_shadow_deathcoil") {
			val artifact = Ulthalesh
		}

		object Demonology extends Spec(266, "Demonology", DPS, "spell_shadow_metamorphosis") {
			val artifact = SkullOfTheManari
		}

		object Destruction extends Spec(267, "Destruction", DPS, "spell_shadow_rainoffire") {
			val artifact = ScepterOfSargeras
		}
	}

	// Monk
	object Monk extends ClassSpecs(Class.Monk) {
		object Brewmaster extends Spec(268, "Brewmaster", Tank, "spell_monk_brewmaster_spec") {
			val artifact = FuZan
		}

		object Mistweaver extends Spec(270, "Mistweaver", Healing, "spell_monk_mistweaver_spec") {
			val artifact = Sheilun
		}

		object Windwalker extends Spec(269, "Windwalker", DPS, "spell_monk_windwalker_spec") {
			val artifact = FistsOfTheHeavens
		}
	}

	// Druid
	object Druid extends ClassSpecs(Class.Druid) {
		object Balance extends Spec(102, "Balance", DPS, "spell_nature_starfall") {
			val artifact = ScytheOfElune
		}

		object Feral extends Spec(103, "Feral", DPS, "ability_druid_catform") {
			val artifact = FangsOfAshamane
		}

		object Guardian extends Spec(104, "Guardian", Tank, "ability_racial_bearform") {
			val artifact = ClawsOfUrsoc
		}

		object Restoration extends Spec(105, "Restoration", Healing, "spell_nature_healingtouch") {
			val artifact = Ghanir
		}
	}

	// Demon Hunter
	object DemonHunter extends ClassSpecs(Class.DemonHunter) {
		object Havoc extends Spec(577, "Havoc", DPS, "ability_demonhunter_specdps") {
			val artifact = TwinbladesOfTheDeceiver
		}

		object Vengeance extends Spec(581, "Vengeance", Tank, "ability_demonhunter_spectank") {
			val artifact = TheAldrachiWarblades
		}
	}

	implicit object JsonFormat extends Format[Spec] {
		def writes(spec: Spec): JsValue = JsNumber(spec.id)
		def reads(json: JsValue): JsResult[Spec] = json.validate[Int].map(fromId)
	}
}
