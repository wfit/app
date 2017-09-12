package gt.workers.updater

import play.api.libs.json.{Format, Json}
import protocol.MessageSerializer

case class Manifest (addons: Seq[Manifest.Addon]) {
	def syncRequired: Boolean = addons.exists(!_.sync)
}

object Manifest {
	val empty = Manifest(Seq.empty)
	case class Addon(name: String, rev: String, date: String, metadata: Metadata,
	                 installed: Boolean = false, managed: Boolean = false,
	                 sync: Boolean = true, latest: String = "?")

	implicit val AddonFormat: Format[Addon] = Json.format[Addon]
	implicit val ManifestFormat: Format[Manifest] = Json.format[Manifest]

	implicit object AddonSerializer extends MessageSerializer.Json(AddonFormat)
	implicit object ManifestSerializer extends MessageSerializer.Json(ManifestFormat)
}
