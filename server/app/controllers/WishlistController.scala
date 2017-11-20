package controllers

import base.{AppComponents, AppController, UserAction, UserRequest}
import db.Toons
import db.api._
import db.wishlist.WishlistsTmp
import javax.inject.{Inject, Singleton}
import models.{Toon, UUID}
import models.wishlist.WishlistTmp

@Singleton
class WishlistController @Inject()(userAction: UserAction)
                                  (cc: AppComponents) extends AppController(cc) {

	private def toons(implicit req: UserRequest[_]): Query[Toons, Toon, Seq] = {
		Toons.filter(t => t.owner === req.user.uuid && t.active)
			.sortBy(t => (t.main.desc, t.ilvl.desc, t.name.asc))
	}

	def wishlist = userAction.async { implicit req =>
		for (ts <- toons.result) yield {
			Ok(views.html.wishlist.wishlist(ts))
		}
	}

	def toon(id: UUID) = userAction.async { implicit req =>
		toons.result.run
         .filter(ts => ts.exists(t => t.uuid == id))
         .flatMap { ts =>
	         WishlistsTmp.filter(w => w.toon === id).map(w => w.text).result.headOption.map { text =>
		         Ok(views.html.wishlist.toon(ts, id, text getOrElse ""))
	         }
	      }
         .recover {
	         case _ => NotFound
         }
	}

	def save(id: UUID) = userAction(parse.json).async { implicit req =>
		toons.result.run
			.filter(ts => ts.exists(t => t.uuid == id))
			.flatMap { _ =>
				val action = WishlistsTmp insertOrUpdate WishlistTmp(id, req.param("text").asString)
				action andThen DBIO.successful(Redirect(routes.WishlistController.wishlist()))
			}
			.recover {
				case _ => NotFound
			}
	}
}
