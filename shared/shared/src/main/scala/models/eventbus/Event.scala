package models.eventbus

import play.api.libs.json.Json
import protocol.{MessageSerializer, SerializerLookup}
import protocol.CompoundMessage._
import protocol.MessageSerializer.Forward

class Event(val channel: String, val payload: String, val system: Boolean = false) {
	def data(implicit decoder: SerializerLookup): Any = MessageSerializer.deserialize(payload)
	def compound: String ~ Forward = channel ~ MessageSerializer.Forward(payload)
	override def toString: String = Event.toJson(channel, payload, system)
}

object Event {
	def apply[D](channel: String, data: D, system: Boolean = false)
	            (implicit serializer: MessageSerializer[D]): Event = {
		new Event(channel, MessageSerializer.serialize(data), system)
	}

	def unapply(event: Event): Option[String] = Some(event.channel)

	def toJson[D](channel: String, payload: String, system: Boolean = false): String = {
		Json.obj("c" -> channel, "d" -> payload, "s" -> system).toString()
	}

	def fromJson(json: String): Event = {
		val obj = Json.parse(json)
		new Event((obj \ "c").as[String], (obj \ "d").as[String], (obj \ "s").as[Boolean])
	}
}
