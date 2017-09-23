package controllers

import controllers.base.AppController
import javax.inject.{Inject, Singleton}
import models.{Toon, Toons, User, Users}
import models.acl.AclView
import scala.concurrent.Future
import utils.SlickAPI._

@Singleton
class RosterController @Inject()() extends AppController {
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
