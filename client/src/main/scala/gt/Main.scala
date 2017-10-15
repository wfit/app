package gt

import scala.scalajs.js

object Main {
	private val g = js.Dynamic.global

	def main(args: Array[String]): Unit = {
		patchRouter()
	}

	private def patchRouter(): Unit = {
		val oldVersioned = g.routes.controllers.Assets.versioned
		g.routes.controllers.Assets.versioned = ((path: String) => {
			oldVersioned(g.versioned.selectDynamic(path) || path.asInstanceOf[js.Dynamic])
		}): js.Function
	}
}
