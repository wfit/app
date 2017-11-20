package services

import akka.Done
import base.{AppComponents, AppService}
import db.acl.AclView
import db.api._
import db.{Credentials, Sessions, Users}
import java.time.Instant
import javax.inject.{Inject, Singleton}
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.cache.{AsyncCacheApi, NamedCache}
import play.api.mvc.Results
import scala.concurrent.Future
import scala.concurrent.duration._
import utils.{UserAcl, UserError}

@Singleton
class AuthService @Inject()(rosterService: RosterService)
                           (cache: AsyncCacheApi, @NamedCache("acl") aclCache: AsyncCacheApi)
                           (cc: AppComponents) extends AppService(cc) {
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
		AclView.filter(e => e.user === user && e.key === "login").map(e => e.value).take(1).result.headOption.flatMap {
			case Some(v) if v > 0 => (Sessions += Session(session, user)) andThen DBIO.successful(session)
			case _ => DBIO.failed(UserError(s"Accès non-autorisé."))
		}.run
	}

	def loadSession(id: UUID): Future[Option[UUID]] = cache.getOrElseUpdate(s"auth:session:$id", 5.minute) {
		val session = Sessions.filter(s => s.id === id)
		(session.map(_.lastAccess).update(Instant.now) andThen session.map(_.user).result.headOption).run
	}

	def loadAcl(userId: UUID): Future[UserAcl] = aclCache.getOrElseUpdate(s"auth:acl:$userId", 15.minutes) {
		AclView.filter(e => e.user === userId)
		.map(e => (e.key, e.value))
		.result
		.map(_.toMap)
		.map(UserAcl.apply)
		.run
	}

	def flushUserAcl(user: UUID): Future[Done] = aclCache.remove(s"auth:acl:$user")
	def flushAclCaches(): Future[Done] = aclCache.removeAll()
}
