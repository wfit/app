package protocol

import play.api.libs.json.Json
import scala.annotation.unchecked.uncheckedVariance

object CompoundMessage {
	case class ~[+A, +B](a: A, b: B)(implicit val as: MessageSerializer[A@uncheckedVariance],
	                                 val bs: MessageSerializer[B@uncheckedVariance])

	implicit class CompoundBuilder[T: MessageSerializer] (lhs: T) {
		def ~[U: MessageSerializer] (rhs: U): T ~ U = new ~(lhs, rhs)
	}

	implicit object Serializer extends MessageSerializer[Any ~ Any] {
		private def item(value: Any)(implicit serializer: MessageSerializer[Any]): String = {
			MessageSerializer.serialize(value)
		}

		private def flatten(value: ~[Any, Any]): Seq[String] = value match {
			case (a: (Any ~ Any)) ~ b => flatten(a) :+ item(b)(value.bs)
			case a ~ b => Seq(item(a)(value.as), item(b)(value.bs))
		}

		def serialize(value: ~[Any, Any]): String = {
			Json.toJson(flatten(value)).toString()
		}

		def deserialize(body: String)(implicit lookup: SerializerLookup): ~[Any, Any] = {
			Json.parse(body).as[List[String]] match {
				case a :: b :: rest =>
					val (ad, as) = MessageSerializer.unpack(a)
					val (bd, bs) = MessageSerializer.unpack(b)
					val first = new ~(ad, bd)(as, bs)
					rest.foldLeft(first) {
						case (prev, c) =>
							val (cd, cs) = MessageSerializer.unpack(c)
							new ~(prev, cd)(this, cs)
					}
				case _ => ???
			}
		}
	}
}
