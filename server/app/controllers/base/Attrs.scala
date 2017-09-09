package controllers.base

import play.api.libs.typedmap.TypedKey

object Attrs {
	val isElectron = TypedKey[Boolean]
	val isFetch = TypedKey[Boolean]
}
