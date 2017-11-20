package graphql

import akka.stream.Materializer
import db.api._
import models.User
import scala.concurrent.{ExecutionContext, Future}
import slick.dbio.{DBIOAction, NoStream}

case class Context(user: User,
						 query: Option[Query] = None,
						 database: Database,
						 executionContext: ExecutionContext,
						 materializer: Materializer) {
	/** Executes an action on the database */
	def run[T](dbio: DBIOAction[T, NoStream, Nothing]): Future[T] = database.run(dbio)
}

object Context {
	/** Constructs a context object from a AuthStatus object */
	def forUser(user: User)(implicit db: Database, ec: ExecutionContext, mat: Materializer): Context = {
		Context(user,
			database = db,
			executionContext = ec,
			materializer = mat)
	}
}
