package gt.modules.composer

import gt.Router
import gt.util.{Http, ViewUtils}
import gt.workers.Worker
import java.util.concurrent.atomic.AtomicInteger
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.DragEvent

class Sidebar extends Worker with ViewUtils {
	private val doc = value[String]("document-id")

	private val tab = Var("none")
	selectTab("roster")

	private def selectTab(selection: String): Unit = {
		tab := selection
		selection match {
			case "roster" => Roster.refresh()
			case _ => // ignore
		}
	}

	private def tabSelected(key: String): Rx[Boolean] = tab.map(_ == key)

	private def genTabs(items: (String, String)*) = items.map { case (key, icon) =>
		<li onclick={() => selectTab(key)} selected={tabSelected(key)}>
			<i>
				{icon}
			</i>
		</li>
	}

	private val tabs = genTabs(
		"roster" -> "people",
		"slacks" -> "flash_on",
		"wishes" -> "star",
		"check" -> "lightbulb_outline"
	)

	private val tree = tab.map {
		case "roster" => Roster.tree
		case _ => <!-- Nothing -->
	}

	private val trashVisible = Var(false)
	private val dndCounter = new AtomicInteger(0)

	private val binding = mount("#composer-sidebar")(
		<div class="tabs-container">
			<ul class="tabs">
				{tabs}
			</ul>
		</div>,
		<div class="flex content"
		     ondragenter={e: dom.DragEvent => dragEnter(e)}
		     ondragover={e: dom.DragEvent => dragOver(e)}
		     ondragleave={e: dom.DragEvent => dragLeave(e)}
		     ondrop={e: dom.DragEvent => drop(e)}>
			<div class="trash" visible={trashVisible}>
				<i>delete_forever</i>
			</div>
			<div>
				{tree}
			</div>
		</div>
	)

	private def dragEnter(e: DragEvent): Unit = {
		dndCounter.incrementAndGet()
		if (Editor.dragType == "slot") {
			e.preventDefault()
			trashVisible := true
		}
	}

	private def dragOver(e: DragEvent): Unit = {
		if (Editor.dragType == "slot") {
			e.preventDefault()
			e.dataTransfer.dropEffect = "move"
		}
	}

	private def dragLeave(e: DragEvent, drop: Boolean = false): Unit = {
		if (drop || dndCounter.decrementAndGet() <= 0) {
			trashVisible := false
			dndCounter.set(0)
		}
	}

	private def drop(e: DragEvent): Unit = {
		if (Editor.dragType == "slot") {
			val frag = Editor.dragSlot.fragment
			val slot = Editor.dragSlot.id
			Http.delete(Router.Composer.deleteSlot(doc, frag, slot))
		}
		dragLeave(e, true)
	}

	def receive: Receive = {
		case _ => ???
	}

	override def onTerminate(): Unit = {
		binding.cancel
	}
}
