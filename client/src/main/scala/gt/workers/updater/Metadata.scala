package gt.workers.updater

import play.api.libs.json._
import scala.util.Try

case class Metadata (hash: String, topLevelDirectories: Set[String]) {
	def toJson: String = Json.toJson(this).toString()
}

object Metadata {
	val empty: Metadata = Metadata("", Set.empty)

	def fromJson(json: String): Metadata = Try(Json.parse(json).as[Metadata]) getOrElse empty

	implicit val format: Format[Metadata] = new Format[Metadata] {
		def reads(json: JsValue): JsResult[Metadata] = JsSuccess(Metadata(
			hash = (json \ "hash").validate[String] getOrElse "",
			topLevelDirectories = (json \ "tld").validate[Set[String]] getOrElse Set.empty
		))

		def writes(o: Metadata): JsValue = Json.obj("hash" -> o.hash, "tld" -> o.topLevelDirectories)
	}
}
