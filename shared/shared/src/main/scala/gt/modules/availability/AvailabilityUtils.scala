package gt.modules.availability
import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, LocalDate, LocalDateTime, LocalTime}

object AvailabilityUtils {
  private val TimeBase = LocalTime.of(6, 0)

  def dateSequence(count: Int, from: LocalDate = null): Seq[LocalDate] = {
    val base = if (from != null) from else LocalDate.now
    Seq.tabulate(count)(offset => base.plus(offset, ChronoUnit.DAYS))
  }

  def dayName(date: LocalDate): String = {
    date.getDayOfWeek match {
      case DayOfWeek.MONDAY    => "Lundi"
      case DayOfWeek.TUESDAY   => "Mardi"
      case DayOfWeek.WEDNESDAY => "Mercredi"
      case DayOfWeek.THURSDAY  => "Jeudi"
      case DayOfWeek.FRIDAY    => "Vendredi"
      case DayOfWeek.SATURDAY  => "Samedi"
      case DayOfWeek.SUNDAY    => "Dimanche"
    }
  }

  val timeSlices: Seq[LocalTime] = {
    Seq.tabulate(24 * 2 - 6)(offset => TimeBase.plus((offset + 6) * 30, ChronoUnit.MINUTES))
  }

  def twoDigits(value: Int): String = if (value < 10) s"0$value" else value.toString

  def rectangle(day1: LocalDate, time1: LocalTime, day2: LocalDate, time2: LocalTime): Seq[(LocalDate, LocalTime)] = {
    val (dayStart, dayEnd)   = if (day1 < day2) (day1, day2) else (day2, day1)
    val (timeStart, timeEnd) = if (time1 < time2) (time1, time2) else (time2, time1)
    (for (day <- dayRange(dayStart, dayEnd); time <- timeRange(timeStart, timeEnd)) yield (day, time)).toSeq
  }

  def nextDay(day: LocalDate): LocalDate   = day.plus(1, ChronoUnit.DAYS)
  def nextTime(time: LocalTime): LocalTime = time.plus(30, ChronoUnit.MINUTES)

  def dayRange(from: LocalDate, to: LocalDate): Iterator[LocalDate] =
    Iterator.iterate(from)(nextDay).takeWhile(day => day <= to)

  def timeRange(from: LocalTime, to: LocalTime): Iterator[LocalTime] =
    Iterator.iterate(from)(nextTime).takeWhile(time => time >= from && time <= to).take(24 * 2)

  implicit final private class LocalDateOps(private val day: LocalDate) extends AnyVal {
    def <(other: LocalDate): Boolean  = day.isBefore(other)
    def <=(other: LocalDate): Boolean = day == other || day < other
  }

  implicit final private class LocalTimeOps(private val time: LocalTime) extends AnyVal {
    def <(other: LocalTime): Boolean = {
      (time.isBefore(TimeBase), other.isBefore(TimeBase)) match {
        case (a, b) if a == b => time.isBefore(other)
        case (_, b)           => b
      }
    }

    def <=(other: LocalTime): Boolean = time == other || time < other
    def >=(other: LocalTime): Boolean = !(time < other)
  }

  def pack(day: LocalDate, time: LocalTime): LocalDateTime = {
    LocalDateTime.of(if (time.isBefore(TimeBase)) nextDay(day) else day, time)
  }

  def unpack(datetime: LocalDateTime): (LocalDate, LocalTime) = {
    val day  = datetime.toLocalDate
    val time = datetime.toLocalTime
    if (time.isBefore(TimeBase)) (day.minus(1, ChronoUnit.DAYS), time) else (day, time)
  }
}
