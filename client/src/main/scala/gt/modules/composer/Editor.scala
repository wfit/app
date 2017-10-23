package gt.modules.composer

import gt.util.View
import gt.workers.Worker
import mhtml.Var
import models.composer.Slot
import utils.UUID

class Editor extends Worker.Dummy with View {
	// Current document object
	//private val document = value[Document]("document")


	Worker.local[Sidebar]
	Worker.local[FragmentsList]
}

object Editor {
	// Drag and drop
	private[composer] var dragType = ""
	private[composer] var dragFragment = UUID.zero
	private[composer] var dragToon = UUID.zero
	private[composer] var dragSlot = null: Slot

}
