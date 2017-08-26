package models

import utils.UUID

case class Toon (uuid: UUID, main: Boolean)

object Toon {
	def dummy(user: User) = Toon(UUID.zero, main = true)
}
