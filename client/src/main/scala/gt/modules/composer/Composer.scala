package gt.modules.composer

import gt.util.{ViewUtils, WorkerView}
import gt.workers.Worker
import models.Toon
import models.composer.Slot
import models.wow.Role
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
	private[composer] var dragType = ""
	private[composer] var dragFragment = UUID.zero
	private[composer] var dragToon = UUID.zero
	private[composer] var dragSlot = null: Slot

	val StandardOrdering: Ordering[(Role, Int, String)] =
		Ordering.by { case (role, ilvl, name) => (role, -ilvl, name) }

	val StandardToonOrdering: Ordering[Toon] =
		StandardOrdering.on(t => (t.spec.role, t.ilvl, t.name))

	val StandardSlotToonOrdering: Ordering[(Slot, Option[Toon])] =
		StandardOrdering.on { case (slot, toon) => (slot.role, toon.map(_.ilvl) getOrElse 0, slot.name) }
}
