package controllers

import base.{AppComponents, AppController}
import db.api._
import db.availability.Availabilities
import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDateTime}
import javax.inject.{Inject, Singleton}
import models.availability.Availability
import play.api.libs.json.JsArray

@Singleton
class AvailabilityController @Inject()(cc: AppComponents) extends AppController(cc) {

  final private val AclKey = "availability.access"

  def availability = (UserAction andThen CheckAcl(AclKey)).async { implicit req =>
    Availabilities.filter(a => a.user === req.user.uuid).result.run.map { mines =>
      Ok(views.html.availability.availability(mines))
    }
  }

  def save = (UserAction andThen CheckAcl(AclKey)).async { implicit req =>
    val today = LocalDateTime.now.truncatedTo(ChronoUnit.DAYS)
    val limit = today.plus(14, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS)
    val ranges = req.body.asJson
      .getOrElse(JsArray.empty)
      .as[Seq[(String, String)]]
      .map {
        case (start, end) => (LocalDateTime.parse(start), LocalDateTime.parse(end))
      }
      .filter {
        case (start, end) if start.isAfter(end)                        => false
        case (start, end) if Duration.between(start, end).toHours > 24 => false
        case (start, _) if start.isBefore(today)                       => false
        case (_, end) if end.isAfter(limit)                            => false
        case _                                                         => true
      }
      .map {
        case (start, end) => Availability(req.user.uuid, start, end)
      }

    val cleanup = Availabilities.filter(a => a.user === req.user.uuid).delete
    val insert  = if (ranges.nonEmpty) Availabilities ++= ranges else DBIO.successful(Some(0))

    (cleanup >> insert).run.map(_ => Created)
  }
}
