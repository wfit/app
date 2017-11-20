package graphql
package schema
package queries

import sangria.schema._

trait Queries extends Root {
	query {
		val uuid = Argument("uuid", UUIDType, "The user ID to fetch")
		Field(
			name = "user",
			fieldType = OptionType(UserType),
			description = "The user with specific id.",
			arguments = List(uuid),
			resolve = implicit env => users.deferOpt(uuid)
		)
	}

	query {
		Field(
			name = "viewer",
			fieldType = OptionType(UserType),
			description = "The current user.",
			resolve = implicit env => users.deferOpt(env.ctx.user.uuid)
		)
	}
}
