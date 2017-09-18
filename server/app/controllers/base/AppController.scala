package controllers.base

import javax.inject.Inject
import play.api.mvc.InjectedController
import scala.concurrent.ExecutionContext

//noinspection VarCouldBeVal
abstract class AppController extends InjectedController {
	@Inject private var userAction: UserAction = null
	@Inject private var executionContext: ExecutionContext = null

	protected def UserAction: UserAction = userAction
	protected implicit def ExecutionContext: ExecutionContext = executionContext
}
