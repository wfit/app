package protocol

import models.UUID
import play.api.libs.json
import play.api.libs.json.{Format, JsValue}
import protocol.Message.CanUseSerializer
import scala.annotation.unchecked.uncheckedVariance
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
trait MessageSerializer[-T] {
	protected final val tag: String = {
		val fqcn = getClass.getName
		MessageSerializer.tagFromName.getOrElse(fqcn, fqcn)
	}

	def serialize(value: T)(implicit cus: CanUseSerializer): String
	def deserialize(body: String)(implicit lookup: SerializerLookup, cus: CanUseSerializer): T@uncheckedVariance

	/** A serializer is symmetric if `deserialize(serialize(foo)) == foo` */
	def symmetric(value: T): Boolean = true

	/** Whether the message was sent optimistically and should not be reported in case of failed delivery */
	def optimistic(value: T): Boolean = false
}

object MessageSerializer {
	// Import implicit CCS to check correct usage of serialize
	private implicit final val cus = CanUseSerializer

	val tagFromName = Map(
		"protocol.MessageSerializer$UnitSerializer$" -> "U",
		"protocol.MessageSerializer$BooleanSerializer$" -> "B",
		"protocol.MessageSerializer$StringSerializer$" -> "S",
		"protocol.MessageSerializer$IntSerializer$" -> "I",
		"protocol.MessageSerializer$DoubleSerializer$" -> "D",
		"protocol.MessageSerializer$JsValueSerializer$" -> "J",
		"protocol.MessageSerializer$UUIDSerializer$" -> "Z",
		"protocol.MessageSerializer$SymbolSerializer$" -> "Y",
		"protocol.MessageSerializer$ForwardSerializer$" -> "F",
		"protocol.CompoundMessage$Serializer$" -> "~"
	)

	val nameFromTag = tagFromName.map { case (n, t) => (t, n) }

	case class Forward(data: String)

	def serialize[T](data: T)(implicit serializer: MessageSerializer[T]): String = {
		data match {
			case Forward(source) => source
			case source =>
				val tag = serializer.tag
				val serialized = serializer.serialize(source)
				if (serialized == null) tag
				else s"$tag:$serialized"
		}
	}

	def deserialize(serialized: String)(implicit lookup: SerializerLookup): Any = unpack(serialized)._1

	def unpack(serialized: String)(implicit lookup: SerializerLookup): (Any, MessageSerializer[Any]) = {
		val (serializer, data) = {
			val colon = serialized.indexOf(":")
			if (colon < 0) (lookup.perform(serialized), null)
			else serialized.splitAt(colon) match { case (t, d) => (lookup.perform(t), d.tail) }
		}
		(serializer.deserialize(data), serializer)
	}

	abstract class Lambda[T](sfn: T => String, dfn: String => T) extends MessageSerializer[T] {
		final def serialize(value: T)(implicit cus: CanUseSerializer): String = {
			sfn(value)
		}

		final def deserialize(value: String)(implicit lookup: SerializerLookup, cus: CanUseSerializer): T = {
			dfn(value)
		}
	}

	abstract class Json[T](val format: Format[T]) extends MessageSerializer[T] {
		final def serialize(value: T)(implicit cus: CanUseSerializer): String = {
			format.writes(value).toString()
		}

		final def deserialize(value: String)(implicit lookup: SerializerLookup, cus: CanUseSerializer): T = {
			format.reads(json.Json.parse(value)).get
		}
	}

	abstract class Singleton[T](singleton: T) extends Lambda[T](_ => "", _ => singleton)

	class Using[T, U](asU: T => U, asT: U => T)
	                 (implicit other: MessageSerializer[U]) extends MessageSerializer[T] {
		def serialize(value: T)(implicit cus: CanUseSerializer): String = {
			other.serialize(asU(value))
		}

		def deserialize(body: String)(implicit lookup: SerializerLookup, cus: CanUseSerializer): T = {
			asT(other.deserialize(body))
		}
	}

	implicit object UnitSerializer extends Singleton[Unit](())
	implicit object BooleanSerializer extends Lambda[Boolean](if (_) "1" else "0", _ == "1")
	implicit object StringSerializer extends Lambda[String](identity, identity)
	implicit object IntSerializer extends Lambda[Int](_.toString, java.lang.Integer.parseInt)
	implicit object DoubleSerializer extends Lambda[Double](_.toString, java.lang.Double.parseDouble)
	implicit object JsValueSerializer extends Json(implicitly[Format[JsValue]])
	implicit object UUIDSerializer extends Lambda[UUID](_.toString, UUID.apply)
	implicit object SymbolSerializer extends Lambda[Symbol](_.name, Symbol.apply)

	// Dummy serializer for Forward objects, will be special-cased in serialize()
	implicit object ForwardSerializer extends MessageSerializer[Forward] {
		def serialize(value: Forward)(implicit ccs: CanUseSerializer): String = ???
		def deserialize(body: String)(implicit lookup: SerializerLookup, cus: CanUseSerializer): Forward = ???
		override def symmetric(value: Forward): Boolean = false
	}
}
