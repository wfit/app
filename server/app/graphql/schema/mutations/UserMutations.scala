package graphql
package schema
package mutations

import sangria.schema.{BooleanType, Field, OptionType}

trait UserMutations { this: Mutations =>
	mutation {
		Field(
			name = "follow",
			fieldType = OptionType(BooleanType),
			description = "",
			arguments = List(),
			resolve = { implicit env =>
				???
			}
		)
	}
}
