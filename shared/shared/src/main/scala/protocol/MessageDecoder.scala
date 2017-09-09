package protocol

trait MessageDecoder {
	def lookupSerializer(fqcn: String): MessageSerializer[Any]
	def decode(tag: String, body: String): Any
}
