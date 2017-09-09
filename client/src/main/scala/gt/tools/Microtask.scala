package gt.tools

import scala.scalajs
import scala.scalajs.js
import scala.scalajs.js.{|, Promise, Thenable}

object Microtask {
	private val p = Promise.resolve[Unit](())
	def schedule(body: => Unit): Unit = p.`then`({ _ => body; (): Unit | Thenable[Unit] }, js.undefined)
}
