package models

import java.time.Instant
import models.wow.{Class, Spec}
import utils.UUID

case class Toon (uuid: UUID, name: String, realm: String, owner: UUID, main: Boolean, active: Boolean,
                 cls: Class, spec: Spec, race: Int, gender: Int, level: Int, thumbnail: Option[String], ilvl: Int,
                 lastUpdate: Instant, invalid: Boolean) {

	def thumbnailUrl: String = {
		thumbnail match {
			case Some(url) => s"https://render-eu.worldofwarcraft.com/character/$url?alt=/forums/static/images/avatars/wow/$race-$gender.jpg"
			case None => "https://eu.battle.net/forums/static/images/avatars/wow/10-0.jpg"
		}
	}

	def renderUrl: String = thumbnailUrl.replaceFirst("avatar", "main")

	def raceName: String = wow.Strings.raceName(race)

	def armoryUrl: String = s"https://worldofwarcraft.com/en-gb/character/$realm/$name".toLowerCase

	def synthetic: Boolean = uuid == UUID.zero
}

object Toon {
	def dummy(user: User) = Toon(
		uuid = UUID.zero,
		name = user.name,
		realm = "—",
		owner = UUID.zero,
		main = true,
		active = true,
		cls = Class.Unknown,
		spec = Spec.Dummy,
		race = 0,
		gender = 0,
		level = 0,
		thumbnail = None,
		ilvl = 0,
		lastUpdate = Instant.now,
		invalid = false
	)
}
