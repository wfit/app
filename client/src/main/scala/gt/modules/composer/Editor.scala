package gt.modules.composer

import gt.util.{ViewUtils, WorkerView}
import gt.workers.Worker
import models.composer.Slot
import utils.UUID

class Editor extends Worker with ViewUtils {
	val doc = value[String]("document-id")

	val sidebar = Worker.local[Sidebar]
	val fragments = Worker.local[FragmentsList]

	def receive: Receive = {
		case None => ???
	}
}

object Editor extends WorkerView[Editor] {
	private[composer] var dragType = ""
	private[composer] var dragFragment = UUID.zero
	private[composer] var dragToon = UUID.zero
	private[composer] var dragSlot = null: Slot
}
