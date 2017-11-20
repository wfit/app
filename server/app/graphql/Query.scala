package graphql

import play.api.libs.json.{JsObject, JsValue, Json}
import sangria.ast.Document
import sangria.parser.{QueryParser, SyntaxError}
import scala.language.implicitConversions
import scala.util.Try

/**
  * A GraphQL query.
  *
  * @param doc  the query document
  * @param op   the operation name
  * @param vars the set of variables
  */
case class Query(doc: Document,
                 op: Option[String] = None,
                 vars: Option[JsObject] = None,
                 id: Option[String] = None)

object Query {
	/** Implicitly builds Queries from Document instances */
	implicit def fromDocument(doc: Document): Query = Query(doc)

	/** Parses a document from JSON source */
	def parseDocument(source: String): Either[JsonError, Document] = {
		QueryParser.parse(source).toEither.left.map {
			case se: SyntaxError => JsonError(se.formattedError)
			case other => JsonError(other.toString)
		}
	}

	/** The default limit for form-data encoded requests */
	final val DefaultBodyLimit = 1024L * 1024L * 10L

	/** Parses the query body from plain text */
	def fromText(body: String): Either[JsonError, Query] = {
		parseDocument(body).map(doc => Query(doc))
	}

	/** Parses the query body from JSON document */
	def fromJSON(body: JsValue): Either[JsonError, Query] = {
		(body \ "query").asOpt[String]
			.toRight(JsonError("This request does not include a GraphQL query."))
			.flatMap { query =>
				val operation = (body \ "operationName").asOpt[String]
				val variables = (body \ "variables").toOption.collect { case obj: JsObject => obj }
				val id = (body \ "id").asOpt[String]
				QueryParser.parse(query).toEither
				.left.map {
					case se: SyntaxError => JsonError(se.formattedError)
					case other => JsonError(other.toString)
				}
				.map(doc => Query(doc))
				.map { query =>
					query.copy(
						op = operation,
						vars = variables,
						id = id
					)
				}
			}
	}

	def decodeVars(vars: String): Option[JsObject] = {
		Try(Json.parse(vars)).toOption.collect { case obj: JsObject => obj }
	}
}
