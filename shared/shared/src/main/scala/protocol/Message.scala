package protocol

import play.api.libs.json.{Format, Json}
import utils.UUID

case class Message private (dest: UUID, sender: UUID, body: String, optimistic: Boolean, ttl: Int = 15) {
	def payload(implicit decoder: SerializerLookup): Any = MessageSerializer.deserialize(body)
	override def toString: String = Json.toJson(this).toString()
}

object Message {
	implicit val format: Format[Message] = Json.format[Message]

	def build[T](dest: UUID, sender: UUID, payload: T)(implicit ms: MessageSerializer[T]): Message = {
		Message(dest, sender, MessageSerializer.serialize(payload), ms.optimistic(payload))
	}

	def parse(input: String): Message = Json.parse(input).as[Message]

	sealed trait CanUseSerializer
	private[protocol] object CanUseSerializer extends CanUseSerializer
}
