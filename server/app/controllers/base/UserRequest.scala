package controllers.base

import controllers.base.UserRequest.Parameter
import models.{Toon, User}
import play.api.libs.json.{JsLookupResult, JsValue, Reads}
import play.api.mvc.{Request, WrappedRequest}
import utils.{UserAcl, UserError, UUID}

case class UserRequest[A] (optUser: Option[User], toons: Seq[Toon], main: Toon, acl: UserAcl,
                           meta: RuntimeMetadata, request: Request[A]) extends WrappedRequest[A](request) {
	def authenticated: Boolean = optUser.isDefined

	val user: User = optUser.orNull

	lazy val isElectron: Boolean = request.headers.get("user-agent").exists(_.contains("Electron"))
	lazy val isFetch: Boolean = request.headers.get("gt-fetch").isDefined

	def param(key: String)(implicit aIsJsValue: A =:= JsValue): Parameter = {
		Parameter(aIsJsValue(request.body) \ key)
	}

	val stateHash: Int = (main.uuid, acl.grants).##

	lazy val autoWorkers: Seq[String] = Seq(
		("gt.workers.ui.UIWorker", true),
		("gt.workers.eventbus.EventBus", authenticated),
		("gt.workers.updater.Updater", isElectron && acl.can("addons.access")),
	).collect {
		case (worker, true) => worker
	}
}

object UserRequest {
	case class Parameter(value: JsLookupResult) extends AnyVal {
		def as[T: Reads]: T = value.as[T]
		def asOpt[T: Reads]: Option[T] = value.asOpt[T]

		def validate[T: Reads](pred: T => Boolean, error: String = "Pré-condition échouée"): T = {
			asOpt[T].filter(pred) getOrElse (throw UserError(error))
		}

		def asBoolean: Boolean = as[Boolean]
		def asString: String = as[String]
		def asInt: Int = as[Int]
		def asUUID: UUID = UUID(as[String])
	}

	object Parameter {
		implicit def convert[T: Reads](param: Parameter): T = param.as[T]
	}
}
