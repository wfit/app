package graphql

import akka.stream.Materializer
import db.api.Database
import sangria.execution.deferred.HasId
import sangria.schema.Argument
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import utils.PrimaryKey

package object schema {
	private[schema] implicit def DatabaseFromContext(implicit ctx: Context): Database = ctx.database
	private[schema] implicit def ExecutionContextFromContext(implicit ctx: Context): ExecutionContext = ctx.executionContext
	private[schema] implicit def MaterializerFromContext(implicit ctx: Context): Materializer = ctx.materializer

	/** Construct an Option[String] from a String value,
	  * this is used to avoid having explicit option for description fields */
	private[schema] implicit def StringToOptionString(value: String): Option[String] = Some(value)

	/** Resolve arguments implicitly when their value type is expected */
	private[schema] implicit def ResolveArgument[T](arg: Argument[T])(implicit env: Env[_]): T = env.arg(arg)

	/** Automatically constructs HasId from PrimaryKeys */
	private[schema] implicit def HasIdFromPrimaryKey[T, K](implicit pk: PrimaryKey[T, K]): HasId[T, K] = pk.get _
}
