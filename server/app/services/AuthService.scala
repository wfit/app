package services

import akka.Done
import javax.inject.{Inject, Singleton}
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.cache.AsyncCacheApi
import play.api.mvc.Results
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import utils.{UserAcl, UserError, UUID}
import utils.SlickAPI._

@Singleton
class AuthService @Inject()(rosterService: RosterService, cache: AsyncCacheApi)
                           (implicit ec: ExecutionContext) {
	private val badAuth = DBIO.failed(UserError("Identifiants incorrects", Results.Unauthorized))

	private def checkPass(cred: Credential, plaintext: String, f: (String, String) => Boolean) = {
		if (f(plaintext, cred.pass)) DBIO.successful(cred)
		else badAuth
	}

	def login(rawId: String, pass: String): Future[UUID] = {
		val id = rawId.toLowerCase.trim
		Credentials.filter { cred =>
			cred.name.toLowerCase === id ||
			cred.name_clean.toLowerCase === id ||
			cred.mail.toLowerCase === id
		}.result.headOption.flatMap {
			case Some(cred) =>
				cred.pass.drop(1).takeWhile(_ != '$').take(5) match {
					case "2a" =>
						checkPass(cred, pass, BCrypt.checkpw)
					case "2y" =>
						checkPass(cred, pass, (plaintext, hashed) =>
							BCrypt.checkpw(plaintext, hashed.replaceFirst("^\\$2y\\$", "\\$2a\\$")))
					case other =>
						DBIO.failed(UserError(s"Mot de passe non supporté: $other"))
				}
			case None =>
				badAuth
		}.flatMap { cred =>
			Users.filter(_.fid === cred.id).result.headOption.map(u => (cred.id, u.map(_.uuid)))
		}.flatMap {
			case (_, Some(uuid)) =>
				DBIO.successful(uuid)
			case (fid, None) =>
				val uuid = UUID.random
				(Users.map(u => (u.uuid, u.fid)) += (uuid, fid)) andThen DBIO.successful(uuid)
		}.transactionally.run
	}

	def createSession(user: UUID): Future[UUID] = {
		val session = UUID.random
		((Sessions += Session(session, user)) andThen DBIO.successful(session)).run
	}

	def loadSession(id: UUID): Future[Option[UUID]] = {
		Sessions.filter(s => s.id === id).map(_.user).headOption
	}

	def loadAcl(userId: UUID): Future[UserAcl] = {
		rosterService.loadUser(userId).flatMap {
			case Some(user) => loadAcl(user)
			case None => Future.successful(UserAcl.empty)
		}
	}

	private class CombinedGrants {
		var min = Int.MaxValue
		var max = Int.MinValue
		var negate = false
		def result: Int = if (negate) min else max
	}

	def loadAcl(user: User): Future[UserAcl] = cache.getOrElseUpdate(s"auth:acl:${ user.uuid }", 5.minutes) {
		val groups = AclGroups.filter { group =>
			val explicitly = AclMemberships.filter(m => group.uuid === m.group && m.user === user.uuid).exists
			val implicitly = group.forumGroup === user.group
			explicitly || implicitly
		}.map(_.uuid)

		val grantsQuery = AclGroupGrants.filter(_.subject in groups).join(AclKeys).on((g, k) => g.key === k.id)
		for (grants <- grantsQuery.run) yield {
			UserAcl(grants.groupBy { case (g, k) => k.key }
				.mapValues { grantsAndKey => grantsAndKey.map { case (grant, key) => grant } }
				.map { case (key, keyGrants) =>
					(key, keyGrants.foldLeft(new CombinedGrants) { (combined, grant) =>
						combined.max = combined.max max grant.value
						combined.min = combined.min min grant.value
						combined.negate = combined.negate || grant.negate
						combined
					}.result)
				})
		}
	}

	def flushUserAcl(user: UUID): Future[Done] = cache.remove(s"auth:acl:$user")
}
