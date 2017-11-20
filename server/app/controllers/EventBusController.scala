package controllers

import base.{AppComponents, AppController}
import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import services.EventBus

@Singleton
class EventBusController @Inject()(eventBus: EventBus)
                                  (cc: AppComponents) extends AppController(cc) {
	def bus = UserAction.authenticated { req =>
		val stream = eventBus.openStream(req.user)
		Ok.chunked(stream.out via EventSource.flow).as(ContentTypes.EVENT_STREAM)
	}

	def subscribe = UserAction.authenticated(parse.json) { req =>
		eventBus.getStream(req.param("stream").asUUID, req.user) match {
			case Some(stream) => req.param("channels").as[Seq[String]].foreach(stream.subscribe); Ok
			case None => NotFound
		}
	}

	def unsubscribe = UserAction.authenticated(parse.json) { req =>
		eventBus.getStream(req.param("stream").asUUID, req.user) match {
			case Some(stream) => req.param("channels").as[Seq[String]].foreach(stream.unsubscribe); Ok
			case None => NotFound
		}
	}
}
