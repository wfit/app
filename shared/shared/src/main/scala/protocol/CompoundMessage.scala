package protocol

import play.api.libs.json.{JsArray, Json}
import scala.annotation.unchecked.uncheckedVariance

object CompoundMessage {
	case class ~[+A, +B](a: A, b: B)(implicit val as: MessageSerializer[A@uncheckedVariance],
	                                 val bs: MessageSerializer[B@uncheckedVariance]) {
	}

	implicit class CompoundBuilder[T: MessageSerializer] (lhs: T) {
		def ~[U: MessageSerializer] (rhs: U): T ~ U = new ~(lhs, rhs)
	}

	implicit object Serializer extends MessageSerializer[Any ~ Any] {
		def serialize(value: ~[Any, Any]): String = {
			Json.toJson(Seq(
				value.as.serialize(value.a), value.as.fqcn,
				value.bs.serialize(value.b), value.bs.fqcn)).toString()
		}

		override def deserialize(body: String, decoder: MessageDecoder): ~[Any, Any] = {
			val Seq(a, Some(as), b, Some(bs)) = Json.parse(body).as[JsArray].value.map(_.asOpt[String])
			new ~(decoder.decode(as, a.orNull), decoder.decode(bs, b.orNull))(decoder.lookupSerializer(as), decoder.lookupSerializer(bs))
		}

		def deserialize(value: String): ~[Any, Any] = ???
	}
}
