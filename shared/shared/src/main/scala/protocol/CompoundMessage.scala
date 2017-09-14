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
		private def item(value: Any, serializer: MessageSerializer[Any]): (String, String) = {
			(serializer.serialize(value), serializer.tag)
		}

		private def flatten(value: ~[Any, Any]): Seq[(String, String)] = value match {
			case (a: (Any ~ Any)) ~ b => flatten(a) :+ item(b, value.bs)
			case a ~ b => Seq(item(a, value.as), item(b, value.bs))
		}

		def serialize(value: ~[Any, Any]): String = {
			Json.toJson(flatten(value)).toString()
		}

		private def decodeTuple(arr: JsArray): (Option[String], String) = {
			(arr(0).asOpt[String], arr(1).as[String])
		}

		override def deserialize(body: String, decoder: MessageDecoder): ~[Any, Any] = {
			Json.parse(body).as[List[JsArray]].map(decodeTuple) match {
				case (a, as) :: (b, bs) :: rest =>
					val first = new ~(decoder.decode(as, a.orNull), decoder.decode(bs, b.orNull))(decoder.lookupSerializer(as), decoder.lookupSerializer(bs))
					rest.foldLeft(first) {
						case (prev, (c, cs)) => new ~(prev, decoder.decode(cs, c.orNull))(this, decoder.lookupSerializer(cs))
					}
				case _ => ???
			}
		}

		def deserialize(value: String): ~[Any, Any] = ???
	}
}
