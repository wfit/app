package platform

import protocol.JsSerializerLookup

object JsPlatform {
	implicit def jsSerializerLookup: JsSerializerLookup.type = JsSerializerLookup
}
