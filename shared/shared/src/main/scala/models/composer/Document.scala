package models.composer

import java.time.Instant
import play.api.libs.json.{Format, Json}
import utils.UUID

case class Document (id: UUID, title: String, updated: Instant)

object Document {
	implicit val format: Format[Document] = Json.format[Document]
}
