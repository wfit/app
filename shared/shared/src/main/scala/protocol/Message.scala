package protocol

import play.api.libs.json.{Format, Json}
import utils.UUID

case class Message private (dest: UUID, sender: UUID, body: Option[String], tag: String, ttl: Int = 15) {
	def payload(implicit decoder: MessageDecoder): Any = decoder.decode(tag, body.orNull)
	override def toString: String = Json.toJson(this).toString()
}

object Message {
	implicit val format: Format[Message] = Json.format[Message]

	def build[T](dest: UUID, sender: UUID, payload: T)(implicit ms: MessageSerializer[T]): Message = {
		Message(dest, sender, Option(ms.serialize(payload)), ms.fqcn)
	}

	def parse(input: String): Message = Json.parse(input).as[Message]
}
