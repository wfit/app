package protocol

import play.api.libs.json
import play.api.libs.json.{Format, JsValue}
import scala.annotation.unchecked.uncheckedVariance
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
trait MessageSerializer[-T] {
	final val tag: String = {
		val fqcn = getClass.getName
		MessageSerializer.tagFromName.getOrElse(fqcn, fqcn)
	}

	def serialize(value: T): String
	def deserialize(body: String): T@uncheckedVariance
	def deserialize(body: String, decoder: MessageDecoder): T@uncheckedVariance = deserialize(body)
}

object MessageSerializer {
	val tagFromName = Map(
		"protocol.MessageSerializer$JsValueSerializer$" -> "J",
		"protocol.MessageSerializer$BooleanSerializer$" -> "B",
		"protocol.MessageSerializer$IntSerializer$" -> "I",
		"protocol.MessageSerializer$DoubleSerializer$" -> "D",
		"protocol.MessageSerializer$StringSerializer$" -> "S",
		"protocol.MessageSerializer$SymbolSerializer$" -> "Y",
		"protocol.MessageSerializer$UnitSerializer$" -> "U",
		"protocol.CompoundMessage$Serializer$" -> "~"
	)

	val nameFromTag = tagFromName.map { case (n, t) => (t, n) }

	abstract class Lambda[T] (sfn: T => String, dfn: String => T) extends MessageSerializer[T] {
		final def serialize(value: T): String = sfn(value)
		final def deserialize(value: String): T = dfn(value)
	}

	abstract class Json[T] (val format: Format[T]) extends MessageSerializer[T] {
		final def serialize(value: T): String = format.writes(value).toString()
		final def deserialize(value: String): T = format.reads(json.Json.parse(value)).get
	}

	abstract class Singleton[T] (singleton: T) extends Lambda[T](_ => "", _ => singleton)

	implicit object JsValueSerializer extends Json(implicitly[Format[JsValue]])
	implicit object BooleanSerializer extends Lambda[Boolean](if (_) "1" else "0", _ == "1")
	implicit object IntSerializer extends Lambda[Int](_.toString, java.lang.Integer.parseInt)
	implicit object DoubleSerializer extends Lambda[Double](_.toString, java.lang.Double.parseDouble)
	implicit object StringSerializer extends Lambda[String](identity, identity)
	implicit object SymbolSerializer extends Lambda[Symbol](_.name, Symbol.apply)
	implicit object UnitSerializer extends Singleton[Unit](())
}
