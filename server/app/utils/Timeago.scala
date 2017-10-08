package utils

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.{Date, Locale}
import org.ocpsoft.prettytime.PrettyTime

object Timeago {
	private[this] val pt: ThreadLocal[PrettyTime] = ThreadLocal.withInitial(() => new PrettyTime(new Locale("fr")))

	def format(instant: Instant): String = pt.get.format(Date.from(instant))
	def format(datetime: LocalDateTime): String = format(datetime.toInstant(ZoneOffset.UTC))
}
