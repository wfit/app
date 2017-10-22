package gt.modules.composer

import gt.util.{ViewUtils, WorkerView}
import gt.workers.Worker
import mhtml.Var
import models.composer.{Document, Slot}
import utils.UUID

class Editor extends Worker.Dummy with ViewUtils {
	// Current document object
	private val document = value[Document]("document")

	// Set the global current editor document
	Editor.document := value[Document]("document")

	Worker.local[Sidebar]
	Worker.local[FragmentsList]
}

object Editor extends WorkerView[Editor] {
	private[composer] var dragType = ""
	private[composer] var dragFragment = UUID.zero
	private[composer] var dragToon = UUID.zero
	private[composer] var dragSlot = null: Slot

	private[composer] val document = Var(null: Document)
}
