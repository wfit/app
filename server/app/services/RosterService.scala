package services

import akka.Done
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import models.{Toon, Toons, User, Users}
import play.api.cache.AsyncCacheApi
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import utils.{FutureOps, UUID}
import utils.SlickAPI._

@Singleton
class RosterService @Inject()(cache: AsyncCacheApi, bnetService: BnetService)
                             (implicit executionContext: ExecutionContext) {
	private val defaultExpires = Duration(5, TimeUnit.MINUTES)

	private def cached[T: ClassTag](key: String, duration: Duration = defaultExpires)
	                               (gen: => Future[T]): Future[T] = {
		cache.getOrElseUpdate(s"roster:$key", defaultExpires)(gen)
	}

	def loadUser(user: UUID): Future[Option[User]] = cached(s"user:$user") {
		Users.filter(_.uuid === user).result.headOption
	}

	def toonsForUser(user: UUID): Future[Seq[Toon]] = cached(s"toons:$user") {
		Toons.filter(_.owner === user).sortBy(t => (t.main.desc, t.active.desc, t.level.desc, t.ilvl.desc, t.name.asc)).result
	}

	def mainForUser(user: User): Future[Toon] = cached(s"main:${user.uuid}") {
		toonsForUser(user).map(toons => toons.find(_.main) getOrElse Toon.dummy(user))
	}

	def bindToon(user: UUID, toon: Toon): Future[Toon] = {
		Toons.filter(t => t.owner === user).exists.result.flatMap { alt =>
			val composed = toon.copy(owner = user, main = !alt, active = true)
			(Toons += composed) andThen composed
		}.run andThenAsync flushUserCaches(user)
	}

	def updateToon(uuid: UUID): Future[Toon] = {
		for {
			old <- Toons.filter(_.uuid === uuid).result.head.run
			toon <- bnetService.fetchToon(old.realm, old.name)
			composed <- updateToon(old, toon)
		} yield composed
	}

	def updateToon(old: Toon, toon: Toon): Future[Toon] = {
		val query = Toons.filter(_.uuid === old.uuid)
		val action = query.map { old =>
			(old.name, old.realm, old.cls, old.spec, old.race, old.gender, old.level, old.thumbnail, old.ilvl, old.lastUpdate, old.invalid, old.failures)
		}.update {
			// Ensure spec is kept only if class is still valid
			val spec = if (old.spec.cls == toon.cls) old.spec else toon.spec
			val ilvl = old.ilvl max toon.ilvl
			(toon.name, toon.realm, toon.cls, spec, toon.race, toon.gender, toon.level, toon.thumbnail, ilvl, Instant.now, false, 0)
		} andThen query.result.head
		action.run.flatMap(t => flushUserCaches(t.owner).replaceSuccess(t))
	}

	def flushUserCaches(user: UUID): Future[Done] = {
		Future.sequence(Seq(
			cache.remove(s"roster:user:$user"),
			cache.remove(s"roster:toons:$user"),
			cache.remove(s"roster:main:$user")
		)).map(_ => Done)
	}
}
