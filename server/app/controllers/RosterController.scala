package controllers

import base.{AppComponents, AppController}
import db.acl.AclView
import db.api._
import db.{Toons, Users}
import javax.inject.{Inject, Singleton}
import models.{Toon, User}
import scala.concurrent.Future

@Singleton
class RosterController @Inject()(cc: AppComponents) extends AppController(cc) {
	private val rosterQuery = (for {
		user <- Users
		rank <- AclView.filter(e => e.user === user.uuid && e.key === "rank").map(_.value)
		toon <- Toons.filter(toon => toon.owner === user.uuid && toon.active)
	} yield (user, rank, toon)).result

	private def formatRoster(roster: Seq[(User, Int, Toon)]): Seq[(User, Int, Seq[Toon])] = {
		roster.groupBy { case (u, r, _) => (u, r) }
			.mapValues { rows => rows.map { case (_, _, t) => t } }
			.map { case ((u, r), toons) => (u, r, toons.sorted) }
			.toSeq
	}

	private def rosterData: Future[Seq[(User, Int, Seq[Toon])]] = rosterQuery.run.map(formatRoster)

	def roster = (UserAction andThen CheckAcl("roster.access")).async { implicit req =>
		rosterData map (data => Ok(views.html.roster.roster(data)))
	}
}
