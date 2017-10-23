package gt.modules.composer

import gt.Router
import gt.modules.composer.sidebar.{DocumentInfo, Dummy, Roster, SidebarTree}
import gt.util.{Http, View}
import gt.workers.Worker
import java.util.concurrent.atomic.AtomicInteger
import mhtml.Var
import models.composer.Document
import org.scalajs.dom
import org.scalajs.dom.DragEvent

class Sidebar extends Worker.Dummy with View {
	/** This document id */
	private val doc = value[Document]("document")

	// Sidebar elements
	lazy val roster = new Roster
	lazy val documentInfo = new DocumentInfo

	/** The current tab key */
	private val tab = Var[String]("roster")

	/** The current tab tree */
	private val tabTree = tab.map[SidebarTree] {
		case "roster" => roster
		case "doc" => documentInfo
		case _ => new Dummy()
	}.flatMap { block =>
		block.refresh()
		block.tree
	}

	/** The list of tabs nodes */
	private val tabs = Seq(
		"roster" -> "people",
		"slacks" -> "flash_on",
		"wishes" -> "star",
		"check" -> "lightbulb_outline",
		"doc" -> "insert_drive_file",
	).map { case (key, icon) =>
		<li onclick={() => tab := key} selected={tab.map(_ == key)}>
			<i>
				{icon}
			</i>
		</li>
	}

	private val trashVisible = Var(false)
	private val dndCounter = new AtomicInteger(0)

	mount("#composer-sidebar")(
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
				{tabTree}
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
			Http.delete(Router.Composer.deleteSlot(doc.id, frag, slot))
		}
		dragLeave(e, drop = true)
	}
}
