package models

import models.wow.WClass
import utils.UUID

case class Toon (uuid: UUID, name: String, realm: String, owner: UUID, main: Boolean, active: Boolean,
                 cls: WClass, race: Int, gender: Int, level: Int, thumbnail: Option[String], ilvl: Int) {
	def thumbnailUrl: String = {
		thumbnail match {
			case Some(url) => s"https://render-eu.worldofwarcraft.com/character/$url?alt=/forums/static/images/avatars/wow/$race-$gender.jpg"
			case None => "https://eu.battle.net/forums/static/images/avatars/wow/10-0.jpg"
		}
	}

	def renderUrl: String = thumbnailUrl.replaceFirst("avatar", "main")
}

object Toon {
	def dummy(user: User) = Toon(
		uuid = UUID.zero,
		name = user.name,
		realm = "—",
		owner = UUID.zero,
		main = true,
		active = true,
		cls = WClass.Undefined,
		race = 0,
		gender = 0,
		level = 0,
		thumbnail = None,
		ilvl = 0
	)
}
