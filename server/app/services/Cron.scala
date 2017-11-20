package services

import akka.actor.ActorSystem
import base.{AppComponents, AppService}
import db.Toons
import db.api._
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._

@Singleton
class Cron @Inject()(as: ActorSystem, rosterService: RosterService)
                    (cc: AppComponents) extends AppService(cc) {

	// Characters updater
	as.scheduler.schedule(15.minutes, 5.minutes) {
		for (batch <- Toons.filter(t => t.active && !t.invalid).sortBy(t => t.lastUpdate.asc).take(5).result.run) {
			for (toon <- batch) yield {
				rosterService.updateToon(toon.uuid)
					.recover { case _ =>
						val failCount = toon.failures + 1
						Toons.filter(t => t.uuid === toon.uuid)
							.map(t => (t.invalid, t.failures, t.lastUpdate))
							.update((failCount >= 2, failCount, Instant.now))
							.run
					}
			}
		}
	}
}
