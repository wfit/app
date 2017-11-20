package graphql

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import sangria.execution.ExecutionScheme.StreamExtended
import sangria.execution._
import sangria.marshalling.playJson.{PlayJsonResultMarshaller, _}
import sangria.streaming.akkaStreams._
import scala.concurrent.ExecutionContext

/**
  * The GraphQL execution engine.
  */
@Singleton
class Engine @Inject()(schema: Schema)
                      (implicit ex: ExecutionContext, mat: Materializer) {
	/** The maximum complexity of a query */
	final val complexityBound = 1000

	/** Executes the given query */
	def execute(query: Query)(implicit ctx: Context): AkkaSource[ExecutionResult[Context, JsValue]] = {
		Executor
		.execute(
			schema = schema.Root,
			queryAst = query.doc,
			userContext = ctx.copy(query = Some(query)),
			operationName = query.op,
			variables = query.vars getOrElse Json.obj(),
			deferredResolver = schema.resolver,
			maxQueryDepth = Some(10),
			queryReducers = List(
				rejectComplexQueries
			),
		)
		.mapError {
			case error: QueryAnalysisError => JsonError(error.resolveError)
			case error: ErrorWithResolver => JsonError(error.resolveError)
		}
	}

	private val rejectComplexQueries: QueryReducer[Context, Context] = {
		QueryReducer.rejectComplexQueries(complexityBound, (c, _) => {
			new IllegalArgumentException(s"This GraphQL query is too complex to be executed: $c > $complexityBound.")
				with UserFacingError
		})
	}
}
