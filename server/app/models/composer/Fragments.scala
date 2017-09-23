package models.composer

import utils.UUID
import utils.SlickAPI._

class Fragments (tag: Tag) extends Table[Fragment](tag, "gt_composer_fragments") {
	def id = column[UUID]("id", O.PrimaryKey)
	def doc = column[UUID]("doc")
	def sort = column[Int]("sort")
	def style = column[Fragment.Style]("style")
	def title = column[String]("title")

	def * = (id, doc, sort, style, title) <> ((Fragment.apply _).tupled, Fragment.unapply)
}

object Fragments extends TableQuery(new Fragments(_))

