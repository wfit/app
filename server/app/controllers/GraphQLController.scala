package controllers

import akka.util.ByteString
import base.{AppComponents, AppController}
import javax.inject.Inject
import play.api.mvc.WebSocket
import scala.concurrent.Future

class GraphQLController @Inject()(cc: AppComponents) extends AppController(cc) {
	def graphql = WebSocket.acceptOrResult[ByteString, ByteString] { req =>
		Future.successful(Left(NotFound))
	}

	def graphiql = Action {
		Ok(views.html.graphiql()).as("text/html; charset=utf-8")
	}
}
