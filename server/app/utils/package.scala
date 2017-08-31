import play.api.mvc.{PathBindable, QueryStringBindable}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

package object utils {
	implicit def UUIDPathBinder(implicit sb: PathBindable[String]): PathBindable[UUID] = new PathBindable[UUID] {
		def bind(key: String, value: String): Either[String, UUID] = sb.bind(key, value).map(UUID.apply)
		def unbind(key: String, value: UUID): String = sb.unbind(key, value.value)
	}

	implicit def UUIDQueryBinder(implicit sb: QueryStringBindable[String]): QueryStringBindable[UUID] = new QueryStringBindable[UUID] {
		def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UUID]] = {
			sb.bind(key, params).map(res => res.map(UUID.apply))
		}
		def unbind(key: String, value: UUID): String = {
			sb.unbind(key, value.value)
		}
	}

	implicit class FutureOps[T] (private val future: Future[T]) extends AnyVal {
		def transformFailure(f: Throwable => Throwable)(implicit ec: ExecutionContext): Future[T] = future.transform(identity, f)
		def transformSuccess[U](f: T => U)(implicit ec: ExecutionContext): Future[U] = future.transform(f, identity)

		def replaceSuccess[U](u: => U)(implicit ec: ExecutionContext): Future[U] = transformSuccess(_ => u)
		def replaceFailure(t: => Throwable)(implicit ec: ExecutionContext): Future[T] = transformFailure(_ => t)

		def andThenAsync[U](f: Try[T] => Future[U])(implicit ec: ExecutionContext): Future[T] = {
			future.transformWith { result =>
				try {
					f match {
						case pf: PartialFunction[Try[T], Future[U]] =>
							pf.applyOrElse[Try[T], Future[Any]](result, _ => Future.successful(())).transform(_ => result)
						case _ =>
							f(result).transform(_ => result)
					}
				} catch {
					case NonFatal(t) =>
						ec reportFailure t
						future
				}
			}
		}

		def andThenAsync[U](f: => Future[U])(implicit ec: ExecutionContext): Future[T] = andThenAsync(_ => f)
	}
}
