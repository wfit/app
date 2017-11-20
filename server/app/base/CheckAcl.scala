package base

import controllers.routes
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckAcl @Inject()(implicit val executionContext: ExecutionContext) {
	def apply(criteria: AclCriterion*): ActionFilter[UserRequest] = new ActionFilter[UserRequest] {
		protected val executionContext: ExecutionContext = CheckAcl.this.executionContext

		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = Future.successful {
			if (criteria.forall(criterion => criterion.check(request.acl))) None
			else if (request.isFetch) Some(Results.Forbidden)
			else Some(Results.Redirect(routes.DashboardController.dashboard()))
		}
	}
}
