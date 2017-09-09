package controllers.base

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckAcl @Inject()(implicit val executionContext: ExecutionContext) {

	private val FNone = Future.successful(None)

	def apply(criteria: AclCriterion*): ActionFilter[UserRequest] = new ActionFilter[UserRequest] {
		protected val executionContext: ExecutionContext = CheckAcl.this.executionContext
		protected def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
			if (criteria.forall(criterion => criterion.check(request.acl, request))) FNone
			else Future.successful(Some(Results.Forbidden))
		}
	}
}
