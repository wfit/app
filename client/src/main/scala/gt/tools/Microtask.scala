package gt.tools

import scala.scalajs.js
import scala.scalajs.js.{|, Promise, Thenable}

object Microtask {
	private val p = Promise.resolve[Unit](())
	private var queue = new js.Array[() => Unit]
	private var scheduled = false

	def schedule(body: => Unit): Unit = {
		queue.push(() => body)
		if (!scheduled) {
			scheduled = true
			p.`then`({ _ =>
				val batch = queue
				queue = new js.Array
				scheduled = false
				for (fn <- batch) fn()
				(): Unit | Thenable[Unit]
			}, js.undefined)
		}
	}
}
