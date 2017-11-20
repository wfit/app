package graphql
package schema

import graphql.schema.deferred.Deferred
import graphql.schema.types.Types
import sangria.schema.{Field, ObjectType, Schema => GraphQLSchema}

trait Root extends Deferred with Types {
	private[this] var queries, mutations, subscriptions = Vector.empty[Field[Context, Unit]]

	private[schema] def query(field: Field[Context, Unit]): Unit = queries :+= field
	private[schema] def mutation(field: Field[Context, Unit]): Unit = mutations :+= field
	private[schema] def subscription(field: Field[Context, Unit]): Unit = subscriptions :+= field

	lazy val Queries = ObjectType("Queries", queries.toList)
	lazy val Mutations = ObjectType("Mutations", mutations.toList)
	lazy val Subscriptions = ObjectType("Subscriptions", subscriptions.toList)

	lazy val Root: GraphQLSchema[Context, Unit] = GraphQLSchema(
		query = Queries,
		mutation = Some(Mutations),
		subscription = Some(Subscriptions)
	)
}
