package utils

import java.time.Instant
import java.util.{Date, Locale}
import org.ocpsoft.prettytime.PrettyTime

object Timeago {
	val pt = new PrettyTime(new Locale("fr"))
	def format(instant: Instant): String = pt.synchronized {
		//pt.setReference(Date.from(Instant.now))
		pt.format(Date.from(instant))
	}

	implicit class Implicitly (private val instant: Instant) extends AnyVal {
		def ago: String = format(instant)
	}
}
