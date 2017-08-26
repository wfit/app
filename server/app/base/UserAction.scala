package base

import akka.http.scaladsl.model.StatusCodes.Redirection
import controllers.routes
import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import services.{AuthService, RosterService}
import utils.{UserAcl, UUID}

class UserAction @Inject()(authService: AuthService, rosterService: RosterService)
                          (val parser: BodyParsers.Default)
                          (implicit val executionContext: ExecutionContext)
	extends ActionBuilder[UserRequest, AnyContent] with ActionTransformer[Request, UserRequest] {

	private def unauthenticatedRequest[A](request: Request[A]): Future[UserRequest[A]] = {
		Future.successful(UserRequest(None, Set.empty, UserAcl.empty, request))
	}

	private def constructUserRequest[A](request: Request[A], userId: UUID): Future[UserRequest[A]] = {
		val user = rosterService.loadUser(userId).map(_.get)
		val toons = rosterService.loadToons(userId)
		val acl = authService.loadAcl(userId)
		for (u <- user; t <- toons; a <- acl) yield UserRequest(Some(u), t, a, request)
	}

	private def loadSession[A](request: Request[A], session: String): Future[UserRequest[A]] = {
		authService.loadSession(UUID(session)).transformWith {
			case Success(Some(user)) => constructUserRequest(request, user)
			case _ => Future.failed(new Exception("Invalid session identifier"))
		}
	}


	protected def transform[A](request: Request[A]): Future[UserRequest[A]] = {
		request.session.get("key").map(loadSession(request, _)) getOrElse unauthenticatedRequest(request)
	}

	private object Authenticated extends ActionFilter[UserRequest] {
		protected def executionContext: ExecutionContext = UserAction.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (request.authenticated) Future.successful(None)
			else Future.successful {
				Some(Results.Redirect(routes.HomeController.login(Some(request.path))))
			}
		}
	}

	private object Unauthenticated extends ActionFilter[UserRequest] {
		protected def executionContext: ExecutionContext = UserAction.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (!request.authenticated) Future.successful(None)
			else Future.successful {
				Some(Results.Redirect(routes.DashboardController.dashboard()))
			}
		}
	}

	val authenticated: ActionBuilder[UserRequest, AnyContent] = this andThen Authenticated
	val unauthenticated: ActionBuilder[UserRequest, AnyContent] = this andThen Unauthenticated
}
