package graphql

import akka.stream.Materializer
import graphql.schema._
import graphql.schema.mutations.Mutations
import graphql.schema.queries.Queries
import graphql.schema.subs.Subscriptions
import javax.inject.{Inject, Singleton}

/**
  * The GraphQL schema definition
  */
@Singleton
class Schema @Inject()(implicit val materializer: Materializer) extends Root
	with Queries
	with Mutations
	with Subscriptions
