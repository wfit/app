package protocol

import scala.scalajs.js
import scala.scalajs.reflect.Reflect

object JsMessageDecoder extends MessageDecoder {
	private val serializerCache = js.Dictionary[MessageSerializer[Any]]()

	def lookupSerializer(tag: String): MessageSerializer[Any] = {
		val fqcn = MessageSerializer.nameFromTag.getOrElse(tag, tag)
		serializerCache.getOrElseUpdate(fqcn,
			Reflect.lookupLoadableModuleClass(fqcn).get.loadModule().asInstanceOf[MessageSerializer[Any]])
	}

	def decode(tag: String, body: String): Any = {
		lookupSerializer(tag).deserialize(body, this)
	}
}
