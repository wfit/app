package protocol

import play.api.libs.json
import play.api.libs.json.{Format, JsResult, JsValue}
import scala.annotation.unchecked.uncheckedVariance
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
trait MessageSerializer[-T] {
	final def fqcn: String = getClass.getName
	def serialize(value: T): String
	def deserialize(body: String): T@uncheckedVariance
	def deserialize(body: String, decoder: MessageDecoder): T@uncheckedVariance = deserialize(body)
}

object MessageSerializer {
	abstract class Lambda[T] (sfn: T => String, dfn: String => T) extends MessageSerializer[T] {
		final def serialize(value: T): String = sfn(value)
		final def deserialize(value: String): T = dfn(value)
	}

	abstract class Json[T] (val format: Format[T]) extends MessageSerializer[T] with Format[T] {
		// Format interface
		final def reads(json: JsValue): JsResult[T] = format.reads(json)
		final def writes(o: T): JsValue = format.writes(o)

		// MessageSerializer interface
		final def serialize(value: T): String = writes(value).toString()
		final def deserialize(value: String): T = reads(json.Json.parse(value)).get
	}

	abstract class Singleton[T] (singleton: T) extends Lambda[T](_ => "<singleton>", _ => singleton)

	implicit object JsValueSerializer extends Json(BasicJsonFormat.JsValueFormat)
	implicit object BooleanSerializer extends Lambda[Boolean](if (_) "1" else "0", _ == "1")
	implicit object IntSerializer extends Lambda[Int](_.toString, java.lang.Integer.parseInt)
	implicit object DoubleSerializer extends Lambda[Double](_.toString, java.lang.Double.parseDouble)
	implicit object StringSerializer extends Lambda[String](identity, identity)
	implicit object SymbolSerializer extends Lambda[Symbol](_.name, Symbol.apply)
}
