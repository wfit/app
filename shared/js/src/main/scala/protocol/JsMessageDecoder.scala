package protocol

import scala.scalajs.js
import scala.scalajs.reflect.Reflect

object JsMessageDecoder extends MessageDecoder {
	private val serializerCache = js.Dictionary[MessageSerializer[Any]]()

	def lookupSerializer(fqcn: String): MessageSerializer[Any] = {
		serializerCache.getOrElseUpdate(fqcn,
			Reflect.lookupLoadableModuleClass(fqcn).get.loadModule().asInstanceOf[MessageSerializer[Any]])
	}

	def decode(tag: String, body: String): Any = {
		lookupSerializer(tag).deserialize(body, this)
	}
}
