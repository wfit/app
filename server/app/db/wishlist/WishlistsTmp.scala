package db.wishlist

import db.api._
import models.UUID
import models.wishlist.WishlistTmp

class WishlistsTmp (tag: Tag) extends Table[WishlistTmp](tag, "gt_wishlist_tmp") {
	def toon = column[UUID]("toon", O.PrimaryKey)
	def text = column[String]("text")

	def * = (toon, text) <> ((WishlistTmp.apply _).tupled, WishlistTmp.unapply)
}

object WishlistsTmp extends TableQuery(new WishlistsTmp(_))
