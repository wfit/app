package db.composer

import db.api._
import java.time.Instant
import models.UUID
import models.composer.Document

class Documents (tag: Tag) extends Table[Document](tag, "gt_composer_docs") {
	def id = column[UUID]("id", O.PrimaryKey)
	def title = column[String]("title")
	def updated = column[Instant]("updated")

	def * = (id, title, updated) <> ((Document.apply _).tupled, Document.unapply)
}

object Documents extends TableQuery(new Documents(_))
