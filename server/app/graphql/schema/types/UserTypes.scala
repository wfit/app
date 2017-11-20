package graphql
package schema
package types

import models.User
import sangria.schema.{Field, ObjectType, fields}

trait UserTypes { this: Types =>
	implicit lazy val UserType: ObjectType[Context, User] = {
		ObjectType(
			"User",
			"The user object",
			() => fields[Context, User](
				Field("uuid", UUIDType, resolve = _.value.uuid),
			)
		)
	}
}
