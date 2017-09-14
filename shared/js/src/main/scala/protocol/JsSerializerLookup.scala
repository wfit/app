package protocol

import scala.scalajs.js
import scala.scalajs.reflect.Reflect

object JsSerializerLookup extends SerializerLookup {
	private val serializerCache = js.Dictionary[MessageSerializer[Any]]()

	def perform(tag: String): MessageSerializer[Any] = {
		val fqcn = MessageSerializer.nameFromTag.getOrElse(tag, tag)
		serializerCache.getOrElseUpdate(fqcn,
			Reflect.lookupLoadableModuleClass(fqcn).get.loadModule().asInstanceOf[MessageSerializer[Any]])
	}
}
