package models.composer

import java.time.Instant
import models.UUID
import play.api.libs.json.{Format, Json}

case class Document(id: UUID, title: String, updated: Instant)

object Document {
	implicit val format: Format[Document] = Json.format[Document]
}
