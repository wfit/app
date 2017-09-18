package controllers.base

import javax.inject.Inject
import play.api.mvc.InjectedController
import scala.concurrent.ExecutionContext

//noinspection VarCouldBeVal
abstract class AppController extends InjectedController {
	@Inject private var userAction: UserAction = null
	@Inject private var checkAcl: CheckAcl = null
	@Inject private var executionContext: ExecutionContext = null

	protected final def UserAction: UserAction = userAction
	protected final def CheckAcl: CheckAcl = checkAcl
	protected final implicit def ExecutionContext: ExecutionContext = executionContext
}
