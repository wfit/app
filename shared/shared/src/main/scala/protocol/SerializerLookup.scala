package protocol

trait SerializerLookup {
	def perform(tag: String): MessageSerializer[Any]
}
