package services

import javax.inject.{Inject, Singleton}
import models.{Toon, User, Users}
import scala.concurrent.{ExecutionContext, Future}
import utils.UUID
import utils.SlickAPI._

@Singleton
class RosterService @Inject()(implicit executionContext: ExecutionContext) {
	def loadUser(user: UUID): Future[Option[User]] = {
		Users.filter(_.uuid === user).headOption
	}

	def loadToons(user: UUID): Future[Set[Toon]] = {
		Future.successful(Set.empty)
	}
}
