package base

import controllers.routes
import javax.inject.{Inject, Singleton}
import models.{Toon, User}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import services.{AuthService, RosterService, RuntimeService}
import utils.{CustomStatus, UserAcl, UUID}

@Singleton
class UserAction @Inject()(authService: AuthService, rosterService: RosterService, runtimeService: RuntimeService)
                          (val parser: BodyParsers.Default)
                          (implicit val executionContext: ExecutionContext)
	extends ActionBuilder[UserRequest, AnyContent] {

	private def instanceUUID: UUID = runtimeService.instanceUUID

	private def unauthenticatedRequest[A](request: Request[A]): Future[UserRequest[A]] = {
		val req = UserRequest(None, Seq.empty, Toon.dummy(User.guest), UserAcl.empty, instanceUUID, request)
		Future.successful(req)
	}

	private def constructUserRequest[A](request: Request[A], userId: UUID): Future[UserRequest[A]] = {
		val user = rosterService.loadUser(userId).map(_.get)
		val toons = rosterService.toonsForUser(userId)
		val main = user.flatMap(rosterService.mainForUser)
		val acl = authService.loadAcl(userId)
		for (u <- user; t <- toons; a <- acl; m <- main) yield UserRequest(Some(u), t, m, a, instanceUUID, request)
	}

	private def loadSession[A](request: Request[A], session: String): Future[UserRequest[A]] = {
		authService.loadSession(UUID(session)).transformWith {
			case Success(Some(user)) => constructUserRequest(request, user)
			case _ => unauthenticatedRequest(request)
		}
	}

	private def transform[A](request: Request[A]): Future[UserRequest[A]] = {
		request.session.get("key").map(loadSession(request, _)) getOrElse unauthenticatedRequest(request)
	}

	def invokeBlock[A](request: Request[A], block: (UserRequest[A]) => Future[Result]): Future[Result] = {
		transform(request).flatMap { req =>
			block(req).map { res =>
				res.withHeaders(
					"Gt-StateHash" -> req.stateHash.toString,
					"Gt-Instance" -> req.instanceUUID.toString,
					"Gt-Method" -> req.method
				)
			}
		}
	}

	private object Authenticated extends ActionFilter[UserRequest] {
		protected def executionContext: ExecutionContext = UserAction.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (request.authenticated) Future.successful(None)
			else Future.successful {
				Some(Results.Redirect(routes.HomeController.login(Some(request.path)), CustomStatus.FullRedirect(request)))
			}
		}
	}

	private object Unauthenticated extends ActionFilter[UserRequest] {
		protected def executionContext: ExecutionContext = UserAction.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (!request.authenticated) Future.successful(None)
			else Future.successful {
				Some(Results.Redirect(routes.DashboardController.dashboard(), CustomStatus.FullRedirect(request)))
			}
		}
	}

	val authenticated: ActionBuilder[UserRequest, AnyContent] = this andThen Authenticated
	val unauthenticated: ActionBuilder[UserRequest, AnyContent] = this andThen Unauthenticated
}
