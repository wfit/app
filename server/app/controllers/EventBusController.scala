package controllers

import akka.stream.Materializer
import controllers.base.UserAction
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc.InjectedController
import services.EventBus

@Singleton
class EventBusController @Inject()(userAction: UserAction, eventBus: EventBus)
                                  (implicit mat: Materializer) extends InjectedController {
	def bus = userAction.authenticated { req =>
		val stream = eventBus.openStream(req.user)
		Ok.chunked(stream.out via EventSource.flow).as(ContentTypes.EVENT_STREAM)
	}

	def subscribe = userAction.authenticated(parse.json) { req =>
		eventBus.getStream(req.param("stream").asUUID, req.user) match {
			case Some(stream) => stream.subscribe(req.param("channel").asString); Ok
			case None => NotFound
		}
	}

	def subscribeAll = userAction { Ok }

	def unsubscribe = userAction.authenticated(parse.json) { req =>
		eventBus.getStream(req.param("stream").asUUID, req.user) match {
			case Some(stream) => stream.unsubscribe(req.param("channel").asString); Ok
			case None => NotFound
		}
	}
}
