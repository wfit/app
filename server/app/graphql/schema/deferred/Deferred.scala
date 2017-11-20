package graphql
package schema
package deferred

import db.Users
import db.api._
import models.{UUID, User}
import sangria.execution.deferred.{DeferredResolver, Fetcher}

trait Deferred {
	val users: Fetcher[Context, User, User, UUID] = Fetcher.caching(
		fetch = (ctx: Context, ids: Seq[UUID]) => {
			ctx.run(Users.filter(u => u.uuid inSet ids).result)
		}
	)

	val resolver: DeferredResolver[Context] = DeferredResolver.fetchers(
		users,
	)
}
