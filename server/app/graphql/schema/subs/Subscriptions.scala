package graphql
package schema
package subs

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import graphql.Context
import java.time.Instant
import sangria.schema._
import sangria.streaming.akkaStreams._
import scala.concurrent.duration._

trait Subscriptions extends Root {
	protected implicit val materializer: Materializer

	subscription {
		Field.subs[Context, Unit, AkkaSource[Action[Context, String]], String, String]("clock", StringType,
			resolve = { implicit env =>
				Source.tick(0.second, 250.millis, ())
				.map(_ => Instant.now().toString)
				.take(15 * 4)
				.map(Value[Context, String])
				.mapMaterializedValue(_ => NotUsed)
			}
		)
	}
}
