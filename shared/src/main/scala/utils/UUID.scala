package utils

import java.util.{UUID => JUUID}

case class UUID (override val toString: String) extends AnyVal

object UUID {
	final val zero = UUID("00000000-0000-0000-0000-000000000000")
	def random: UUID = UUID(JUUID.randomUUID().toString)
}
