package base

import models.{Toon, User}
import play.api.mvc.{Request, WrappedRequest}
import utils.UserAcl

case class UserRequest[A] (optUser: Option[User], toons: Set[Toon], acl: UserAcl,
                           request: Request[A]) extends WrappedRequest[A](request) {
	def authenticated: Boolean = optUser.isDefined

	val user: User = optUser.orNull
	val main: Toon = toons.find(_.main) getOrElse optUser.map(Toon.dummy).orNull

	lazy val isElectron: Boolean = request.headers.get("user-agent").exists(_.contains("Electron"))
	lazy val isEmbedded: Boolean = request.headers.get("gt-embedded").isDefined
}
