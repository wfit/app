package models.composer

import java.time.LocalDateTime
import utils.UUID

case class Document (id: UUID, title: String, updated: LocalDateTime)
