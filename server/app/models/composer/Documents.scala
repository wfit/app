package models.composer

import java.time.Instant
import utils.SlickAPI._
import utils.UUID

class Documents (tag: Tag) extends Table[Document](tag, "gt_composer_docs") {
	def id = column[UUID]("id", O.PrimaryKey)
	def title = column[String]("title")
	def updated = column[Instant]("updated")

	def * = (id, title, updated) <> ((Document.apply _).tupled, Document.unapply)
}

object Documents extends TableQuery(new Documents(_))
