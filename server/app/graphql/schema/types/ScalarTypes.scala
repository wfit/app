package graphql.schema.types

import graphql.schema.types.ScalarTypes.{CoercionViolation, SimpleScalar}
import java.time.Instant
import models.UUID
import sangria.ast.{StringValue, Value}
import sangria.schema.ScalarType
import sangria.validation.{ValueCoercionViolation, Violation}
import scala.util.Try

/**
  * Custom scalar types definitions.
  */
trait ScalarTypes { this: Types =>
	implicit lazy val UUIDType: ScalarType[UUID] = SimpleScalar[UUID, String](
		"UUID",
		"A RFC 4122 identifier. The raw 128-bit value must be represented as 32 hexadecimal digits, in five " +
			"groups separated by hyphens, in the form `8-4-4-4-12`. For example: `2cb8f0e9-7f29-4920-9d75-c18b23a226d8`",
		uuid => uuid.toString,
		UUID.apply _ andThen Right.apply
	)

	implicit lazy val URIType: ScalarType[String] = SimpleScalar[String, String](
		"URI",
		"An RFC 3986, RFC 3987, and RFC 6570 (level 4) compliant URI string.",
		identity,
		Right.apply
	)

	implicit lazy val TimestampType: ScalarType[Instant] = SimpleScalar[Instant, String](
		"Timestamp",
		"An ISO 8601 compliant URI combined date and time in UTC string.",
		instant => instant.toString,
		string => Try(Instant.parse(string)).toEither.left.map(_ => CoercionViolation("Invalid Timestamp format"))
	)
}

object ScalarTypes {
	/** A coercion went wrong! */
	case class CoercionViolation(reason: String) extends ValueCoercionViolation(reason)

	/** Type-classes encoding representational value extraction logic for a type [[R]] */
	trait AsRepr[R] {
		def fromValue(typeName: String, input: Any): Either[Violation, R]
		def fromNode(typeName: String, node: Value): Either[Violation, R]
	}

	object AsRepr {
		/** Constructs an instance of [[AsRepr]] based on two partial functions,
		  * the coercion will fail with a "$tpe expected" error is the pf is undefined for a value */
		def apply[R](fv: PartialFunction[Any, R],
		             fn: PartialFunction[Value, R]): AsRepr[R] = new AsRepr[R] {
			def fromValue(typeName: String, input: Any): Either[Violation, R] =
				fv.lift(input).toRight(CoercionViolation(s"$typeName expected"))

			def fromNode(typeName: String, node: Value): Either[Violation, R] =
				fn.lift(node).toRight(CoercionViolation(s"$typeName expected"))
		}

		// Provides an instance for string, the only type used for now
		implicit val StringRepr: AsRepr[String] = AsRepr({ case s: String => s }, { case StringValue(s, _, _) => s })
	}

	/**
	  * Defines a simple scalar type [[T]] backed by a JSON representation of type [[Repr]].
	  * Leverages an implicit instance of type-class [[AsRepr]] to reduce boilerplate code.
	  *
	  * @param name the name of the type being defined
	  * @param desc a description for the type
	  * @param out  a conversion from the Scala value to the representational value
	  * @param in   a conversion from the representational value to the Scala value
	  * @tparam T    the Scala type for this GraphQL type
	  * @tparam Repr the representational JSON type for this type
	  *
	  * @return an instance of ScalarType[T] defining the type at the Sangria-level
	  */
	def SimpleScalar[T, Repr](name: String, desc: String, out: T => Repr, in: Repr => Either[Violation, T])
	                         (implicit repr: AsRepr[Repr]): ScalarType[T] = {
		ScalarType[T](
			name,
			Some(desc),
			coerceOutput = (value: T, _) => out(value),
			coerceUserInput = (value: Any) => repr.fromValue(name, value).flatMap(in),
			coerceInput = (node: Value) => repr.fromNode(name, node).flatMap(in),
		)
	}
}
