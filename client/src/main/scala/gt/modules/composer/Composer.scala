package gt.modules.composer

import gt.util.{ViewUtils, WorkerView}
import gt.workers.Worker
import utils.UUID

class Composer extends Worker with ViewUtils {
	val doc = value[String]("document-id")

	val sidebar = Worker.local[Sidebar]
	val fragments = Worker.local[FragmentsList]

	def receive: Receive = {
		case None => ???
	}
}

object Composer extends WorkerView[Composer] {
	var dragType = ""
	var dragFragment = UUID.zero
}
