import play.api.mvc.Result
import scala.concurrent.Future

package object controllers {
	implicit def implicitFutureResult(res: Result): Future[Result] = Future.successful(res)
}
