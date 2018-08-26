package models.availability
import java.time.{Instant, LocalDateTime, ZoneOffset}
import models.UUID
import play.api.libs.functional.syntax._
import play.api.libs.json._

final case class Availability(user: UUID, from: LocalDateTime, to: LocalDateTime)

object Availability {
  private val reads: Reads[Availability] = Json.reads[(UUID, Instant, Instant)].map {
    case (user, from, to) =>
      Availability(user, LocalDateTime.ofInstant(from, ZoneOffset.UTC), LocalDateTime.ofInstant(to, ZoneOffset.UTC))
  }

  private val writes: Writes[Availability] = Json.writes[(UUID, Instant, Instant)].contramap[Availability] { a =>
    (a.user, a.from.toInstant(ZoneOffset.UTC), a.to.toInstant(ZoneOffset.UTC))
  }

  implicit val format: Format[Availability] = Format(reads, writes)
}
