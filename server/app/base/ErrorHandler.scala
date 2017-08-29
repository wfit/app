package base

import javax.inject.{Inject, Provider, Singleton}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import utils.UserError

@Singleton
class ErrorHandler @Inject() (env: Environment, config: Configuration,
                              sourceMapper: OptionalSourceMapper, router: Provider[Router])
                             (implicit ec: ExecutionContext)
	extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

	@tailrec private def selectUserError(exception: Throwable): Option[UserError] = exception match {
		case ee: ExecutionException => selectUserError(ee.getCause)
		case ue: UserError => Some(ue)
		case _ => None
	}

	override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = Future.successful {
		selectUserError(exception)
			.map { case UserError(msg, status) => status(msg) }
			.getOrElse(Results.InternalServerError(exception.getMessage))
		//super.onServerError(request, exception)
	}
}
