package models.composer

import java.time.LocalDateTime
import utils.UUID
import utils.SlickAPI._

class Documents (tag: Tag) extends Table[Document](tag, "gt_composer_docs") {
	def id = column[UUID]("id", O.PrimaryKey)
	def title = column[String]("title")
	def updated = column[LocalDateTime]("updated")

	def * = (id, title, updated) <> (Document.tupled, Document.unapply)
}

object Documents extends TableQuery(new Documents(_))
