package base

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, RequestHeader, Result}
import scala.concurrent.{ExecutionContext, Future}
import services.RuntimeService

class GuildToolsFilter @Inject()(runtimeService: RuntimeService)
                                (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
	private def detectElectron(rh: RequestHeader): RequestHeader = {
		rh.addAttr(Attrs.isElectron, rh.headers.get("user-agent").exists(_.contains("Electron")))
	}

	private def detectFetch(rh: RequestHeader): RequestHeader = {
		rh.addAttr(Attrs.isFetch, rh.headers.get("gt-fetch").isDefined)
	}

	private def addInstanceHeader(result: Result): Result = {
		result.withHeaders("Gt-Instance" -> runtimeService.instanceUUID.toString)
	}

	private def formatNanoTime(dt: Long): String = {
		((dt / 1000).toDouble / 1000).toString
	}

	private def addTimeHeader(start: Long)(result: Result): Result = {
		result.withHeaders("Gt-Time" -> formatNanoTime(System.nanoTime() - start))
	}

	def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
		val start = System.nanoTime()
		next(detectFetch(detectElectron(rh)))
			.map(addInstanceHeader)
			.map(addTimeHeader(start))
	}
}
