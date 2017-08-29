package base

import base.UserRequest.Parameter
import models.{Toon, User}
import play.api.libs.json.{JsLookupResult, JsValue, Reads}
import play.api.mvc.{Request, WrappedRequest}
import utils.UserAcl

case class UserRequest[A] (optUser: Option[User], toons: Seq[Toon], main: Toon, acl: UserAcl,
                           request: Request[A]) extends WrappedRequest[A](request) {
	def authenticated: Boolean = optUser.isDefined

	val user: User = optUser.orNull

	lazy val isElectron: Boolean = request.headers.get("user-agent").exists(_.contains("Electron"))
	lazy val isFetch: Boolean = request.headers.get("gt-fetch").isDefined

	def param(key: String)(implicit aIsJsValue: A =:= JsValue): Parameter = {
		Parameter(aIsJsValue(request.body) \ key)
	}
}

object UserRequest {
	case class Parameter(value: JsLookupResult) extends AnyVal {
		def as[T: Reads]: T = value.as[T]

		def asBoolean: Boolean = as[Boolean]
		def asString: String = as[String]
		def asInt: Int = as[Int]
	}

	object Parameter {
		implicit def convert[T: Reads](param: Parameter): T = param.as[T]
	}
}
