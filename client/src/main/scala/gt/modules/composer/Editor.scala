package gt.modules.composer

import gt.util.View
import gt.workers.Worker
import mhtml.Var
import models.composer.Slot
import utils.UUID

class Editor extends Worker.Dummy with View {
	// Current document object
	//private val document = value[Document]("document")

	// Global options
	val filterMains = Var(false)
	val showStats = Var(false)
	val showDuplicates = Var(false)

	// Set the global editor reference
	Editor.current := this

	// Spawn sub-components
	Worker.local[Sidebar]
	Worker.local[FragmentsList]
}

object Editor {
	// Drag and drop
	private[composer] var dragType = ""
	private[composer] var dragFragment = UUID.zero
	private[composer] var dragToon = UUID.zero
	private[composer] var dragSlot = null: Slot

	// Current editor
	private[composer] val current = Var(null: Editor)

	// Options
	private[composer] val filterMains = current.flatMap(_.filterMains)
	private[composer] val showStats = current.flatMap(_.showStats)
	private[composer] val showDuplicates = current.flatMap(_.showDuplicates)
}
