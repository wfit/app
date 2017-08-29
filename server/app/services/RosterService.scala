package services

import akka.Done
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import models.{Toon, Toons, User, Users}
import play.api.cache.AsyncCacheApi
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import utils.UUID
import utils.SlickAPI._

@Singleton
class RosterService @Inject()(cache: AsyncCacheApi)
                             (implicit executionContext: ExecutionContext) {
	private val defaultExpires = Duration(5, TimeUnit.MINUTES)

	private def cached[T: ClassTag](key: String, duration: Duration = defaultExpires)
	                               (gen: => Future[T]): Future[T] = {
		cache.getOrElseUpdate(s"roster:$key", defaultExpires)(gen)
	}

	def loadUser(user: UUID): Future[Option[User]] = cached(s"user:$user") {
		Users.filter(_.uuid === user).headOption
	}

	def toonsForUser(user: UUID): Future[Seq[Toon]] = cached(s"toons:$user") {
		Toons.filter(_.owner === user)
			.sortBy(t => (t.main.desc, t.active.desc, t.level.desc, t.ilvl.desc, t.name.asc))
			.run
	}

	def mainForUser(user: User): Future[Toon] = cached(s"main:$user.uuid") {
		toonsForUser(user).map(toons => toons.find(_.main) getOrElse Toon.dummy(user))
	}

	def bindToon(user: UUID, toon: Toon): Future[Toon] = {
		Toons.filter(t => t.owner === user).exists.result.flatMap { alt =>
			val composed = toon.copy(owner = user, main = !alt, active = true)
			(Toons += composed) andThen DBIO.from(flushUserCaches(user)) andThen DBIO.successful(composed)
		}.run
	}

	def flushUserCaches(user: UUID): Future[Done] = {
		Future.sequence(Seq(
			cache.remove(s"roster:user:$user"),
			cache.remove(s"roster:toons:$user"),
			cache.remove(s"roster:main:$user")
		)).map(_ => Done)
	}
}
