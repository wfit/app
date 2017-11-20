package graphql.schema.types

import graphql.schema.deferred.Deferred

trait Types extends Deferred
	with ScalarTypes
	with UserTypes
