package services

import java.time.Instant
import javax.inject.{Inject, Singleton}
import models.Toon
import models.wow.Class
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.{ExecutionContext, Future}
import utils.{UserError, UUID}

@Singleton
class BnetService @Inject()(conf: Configuration, ws: WSClient)
                           (implicit executionContext: ExecutionContext) {
	private final val base = "https://eu.api.battle.net/"
	private val key = conf.get[String]("bnet.key")
	private val secret = conf.get[String]("bnet.secret")

	def fetchToon(realm: String, name: String): Future[Toon] = {
		ws.url(s"$base/wow/character/$realm/$name".toLowerCase)
			.addQueryStringParameters("apikey" -> key, "fields" -> "items")
			.get()
			.collect {
				case res if res.status == 200 => res.json
				case _ => throw UserError("Ce personnage est introuvable.")
			}
			.map { data =>
				val clss = Class.fromId((data \ "class").as[Int])
				val spec = clss.specs.head
				Toon(
					uuid = UUID.random,
					name = (data \ "name").as[String],
					realm = (data \ "realm").as[String],
					owner = UUID.zero,
					main = false,
					active = true,
					cls = clss,
					spec = spec,
					race = (data \ "race").as[Int],
					gender = (data \ "gender").as[Int],
					level = (data \ "level").as[Int],
					thumbnail = (data \ "thumbnail").asOpt[String],
					ilvl = (data \ "items" \ "averageItemLevelEquipped").as[Int],
					lastUpdate = Instant.now,
					invalid = false,
					failures = 0
				)
			}
	}
}
