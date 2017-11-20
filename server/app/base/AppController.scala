package base

import db.api._
import play.api.mvc.{AbstractController, Result}
import scala.concurrent.Future

/**
  * Base class for a Play Controller module.
  *
  * @param cc an injected instance of AppComponents
  */
abstract class AppController(private[base] val cc: AppComponents)
	extends AbstractController(cc) with AppComponents.Implicits {

	protected final val UserAction: UserAction = cc.userAction
	protected final val CheckAcl: CheckAcl = cc.checkAcl

	protected final implicit def ResultFromDBIO(action: DBIOAction[Result, NoStream, Nothing]): Future[Result] = action.run
	protected final implicit def FutureFromResult(res: Result): Future[Result] = Future.successful(res)
}
