import play.api.mvc.PathBindable
import scala.concurrent.{ExecutionContext, Future}

package object utils {
	implicit object UUIDBinder extends PathBindable[UUID] {
		def bind(key: String, value: String): Either[String, UUID] = Right(UUID(value))
		def unbind(key: String, value: UUID): String = value.value
	}

	implicit class FutureOps[T] (private val future: Future[T]) extends AnyVal {
		def transformFailure(f: Throwable => Throwable)(implicit ec: ExecutionContext): Future[T] = future.transform(identity, f)
		def transformSuccess[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = future.transform(f, identity)
		
		def replaceSuccess[U](u: => U)(implicit ec: ExecutionContext): Future[U] = transformSuccess(_ => u)
		def replaceFailure(t: => Throwable)(implicit ec: ExecutionContext): Future[T] = transformFailure(_ => t)
	}
}
