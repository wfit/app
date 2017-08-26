package facades.html5

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess
import scala.language.dynamics

@js.native
sealed trait Dataset extends js.Any with scala.Dynamic {
	@JSBracketAccess
	def selectDynamic(name: String): js.UndefOr[String] = js.native

	@JSBracketAccess
	def updateDynamic(name: String)(value: String): Unit = js.native
}
