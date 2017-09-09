package platform

import protocol.JsMessageDecoder

object JsPlatform {
	implicit def jsMessageDecoder: JsMessageDecoder.type = JsMessageDecoder
}
