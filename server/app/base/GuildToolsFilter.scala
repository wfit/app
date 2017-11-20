package base

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}
import scala.concurrent.{ExecutionContext, Future}

class GuildToolsFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
	private def formatNanoTime(dt: Long): String = {
		((dt / 1000).toDouble / 1000).toString
	}

	private def addTimeHeader(start: Long)(result: Result): Result = {
		result.withHeaders("Gt-Time" -> formatNanoTime(System.nanoTime() - start))
	}

	def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
		val start = System.nanoTime()
		next(rh).map(addTimeHeader(start))
	}
}
