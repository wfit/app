package utils

import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonFormats {
	implicit def tuple2Reads[A: Reads, B: Reads]: Reads[(A, B)] =
		(JsPath(0).read[A] and
		 JsPath(1).read[B]).tupled

	implicit def tuple2Writes[A: Writes, B: Writes]: Writes[(A, B)] =
		Writes { case (a, b) => Json.arr(a, b)}

	implicit def tuple2Format[A: Format, B: Format]: Format[(A, B)] =
		Format(tuple2Reads, tuple2Writes)

	implicit def tuple3[A: Format, B: Format, C: Format]: Format[(A, B, C)] =
		(JsPath(0).format[A] and
		 JsPath(1).format[B] and
		 JsPath(2).format[C]).tupled

	implicit def tuple4[A: Format, B: Format, C: Format, D: Format]: Format[(A, B, C, D)] =
		(JsPath(0).format[A] and
		 JsPath(1).format[B] and
		 JsPath(2).format[C] and
		 JsPath(4).format[D]).tupled

	implicit def tuple5[A: Format, B: Format, C: Format, D: Format, E: Format]: Format[(A, B, C, D, E)] =
		(JsPath(0).format[A] and
		 JsPath(1).format[B] and
		 JsPath(2).format[C] and
		 JsPath(4).format[D] and
		 JsPath().format[E]).tupled

	implicit def option[A: Format]: Format[Option[A]] = new Format[Option[A]] {
		def writes(o: Option[A]): JsValue = o.map(Json.toJson(_)).getOrElse(JsNull)
		def reads(json: JsValue): JsResult[Option[A]] = json match {
			case JsNull => JsSuccess(None)
			case other => Json.fromJson[A](other).map(Some.apply)
		}
	}
}
