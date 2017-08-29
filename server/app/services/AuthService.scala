package services

import javax.inject.{Inject, Singleton}
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc.Results
import scala.concurrent.{ExecutionContext, Future}
import utils.{UserAcl, UserError, UUID}
import utils.SlickAPI._

@Singleton
class AuthService @Inject()(implicit ec: ExecutionContext) {
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

	def loadAcl(user: UUID): Future[UserAcl] = Future.successful(UserAcl.empty)
}
