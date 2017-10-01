package gt.modules.composer

import gt.modules.composer.fragments.{FragmentTree, Grid, Group, Text}
import gt.util.{Http, ViewUtils}
import gt.workers.Worker
import gt.workers.eventbus.EventBus
import java.util.concurrent.atomic.AtomicInteger
import mhtml.Var
import models.composer.Fragment
import org.scalajs.dom
import org.scalajs.dom.{html, FocusEvent, KeyboardEvent, MouseEvent}
import play.api.libs.json.Json
import scala.collection.mutable
import scala.xml.Elem
import utils.UUID

class FragmentsList extends Worker with ViewUtils {
	// The current composer document
	val doc = value[String]("document-id")

	// Subscribes to fragments refresh events
	val RefreshEventKey = s"composer:$doc:fragments.refresh"
	EventBus.subscribe(RefreshEventKey)

	// The sequence of fragments of this document
	val fragments = Var(Seq.empty[Fragment])
	refreshFragments()

	/** Refreshes the fragment list */
	def refreshFragments(): Unit = {
		for (res <- Http.get(s"/composer/$doc/fragments")) {
			fragments := res.json.as[Seq[Fragment]]
		}
	}

	/** The icon for the given fragment style */
	private def fragmentIcon(style: Fragment.Style): String = style match {
		case Fragment.Text => "text_fields"
		case Fragment.Group => "people"
		case Fragment.Grid => "border_all"
	}

	/** A cache of fragment implementations instances */
	private val fragmentTreeCache = mutable.Map.empty[UUID, FragmentTree]

	/** The DOM tree for a given fragment */
	private def treeForFragment(fragment: Fragment): Elem = {
		fragmentTreeCache.getOrElseUpdate(fragment.id, {
			fragment.style match {
				case Fragment.Text => Text(fragment)
				case Fragment.Group => Group(fragment)
				case Fragment.Grid => Grid(fragment)
			}
		}).tree
	}

	/** Builds the DOM block for a specific fragment */
	private def fragmentBlock(fragment: Fragment): Elem = {
		val counter = new AtomicInteger(0)
		<div class="fragment"
		     ondragenter={e: dom.DragEvent => fragmentDragEnter(e, counter, fragment.id)}
		     ondragover={e: dom.DragEvent => fragmentDragOver(e, fragment.id)}
		     ondragleave={e: dom.DragEvent => fragmentDragLeave(e, counter)}
		     ondrop={(e: dom.DragEvent) => fragmentDragDrop(e, counter, fragment.id)}>
			<h3 draggable="true"
			    ondragstart={e: dom.DragEvent => fragmentDragStart(e, fragment.id)}
			    ondragend={() => fragmentDragEnd()}>
				<i>
					{fragmentIcon(fragment.style)}
				</i>
				<span onmouseup={e: dom.MouseEvent => fragmentTitleMouseUp(e)}
				      onkeydown={e: dom.KeyboardEvent => fragmentTitleKeyDown(e)}
				      onblur={e: dom.FocusEvent => fragmentTitleBlur(e, fragment)}>
					{fragment.title}
				</span>
				<button class="btn alternate" onclick={() => deleteFragment(fragment)}>
					<i>delete_forever</i>
				</button>
			</h3>
			<div>
				{treeForFragment(fragment)}
			</div>
		</div>
	}

	/** A fragment starts being dragged */
	private def fragmentDragStart(event: dom.DragEvent, id: UUID): Unit = {
		Composer.dragType = "fragment"
		Composer.dragFragment = id
		event.dataTransfer.dropEffect = "move"
	}

	/** A fragment stops being dragged */
	private def fragmentDragEnd(): Unit = {
		Composer.dragType = null
	}

	/** Something started being dragged over a fragment */
	private def fragmentDragEnter(event: dom.DragEvent, counter: AtomicInteger, id: UUID): Unit = {
		counter.incrementAndGet()
		val el = event.currentTarget.asInstanceOf[html.Element]
		if (Composer.dragType == "fragment") {
			event.preventDefault()
		} else if (Composer.dragType == "toon") {
			el.classList.add("drag-toon")
		}
	}

	/** Something is being dragged over a fragment */
	private def fragmentDragOver(event: dom.DragEvent, id: UUID): Unit = {
		val el = event.currentTarget.asInstanceOf[html.Element]
		if (Composer.dragType == "fragment" && Composer.dragFragment != id) {
			event.preventDefault()
			val rect = el.getBoundingClientRect()
			val y = event.clientY - rect.top
			if (y < rect.height / 2) {
				el.classList.add("drag-before")
				el.classList.remove("drag-after")
			} else {
				el.classList.remove("drag-before")
				el.classList.add("drag-after")
			}
		}
	}

	/** Something stopped being dragged over a fragment */
	private def fragmentDragLeave(event: dom.DragEvent, counter: AtomicInteger, drop: Boolean = false): Unit = {
		if (drop || counter.decrementAndGet() <= 0) {
			val el = event.currentTarget.asInstanceOf[html.Element]
			el.classList.remove("drag-before")
			el.classList.remove("drag-after")
			el.classList.remove("drag-toon")
			counter.set(0)
		}
	}

	/** Something was dropped over a fragment */
	private def fragmentDragDrop(event: dom.DragEvent, counter: AtomicInteger, id: UUID): Unit = {
		if (Composer.dragType == "fragment" && Composer.dragFragment != id) {
			val el = event.currentTarget.asInstanceOf[html.Element]
			val rect = el.getBoundingClientRect()
			Http.post(s"/composer/$doc/moveFragment", Json.obj(
				"source" -> Composer.dragFragment,
				"target" -> id,
				"position" -> (if (event.clientY - rect.top < rect.height / 2) "before" else "after")
			))
		}
		fragmentDragLeave(event, counter, drop = true)
	}

	/** The fragment title was clicked without moving the mouse */
	private def fragmentTitleMouseUp(event: MouseEvent): Unit = {
		val el = event.currentTarget.asInstanceOf[html.Element]
		if (!el.hasAttribute("contenteditable")) {
			el.textContent = el.textContent.trim
			el.setAttribute("contenteditable", "true")
			el.setAttribute("old-title", el.textContent)
			el.focus()
			dom.document.execCommand("selectAll", false)
		}
	}

	/** Key down event while editing a fragment title */
	private def fragmentTitleKeyDown(event: KeyboardEvent): Unit = {
		val el = event.currentTarget.asInstanceOf[html.Element]
		if (event.keyCode == 13) { // Enter
			event.preventDefault()
			el.blur()
		} else if (event.ctrlKey || event.altKey) {
			event.preventDefault()
		}
	}

	/** Focus out of a fragment title */
	private def fragmentTitleBlur(event: FocusEvent, fragment: Fragment): Unit = {
		val el = event.currentTarget.asInstanceOf[html.Element]

		val title = el.textContent.trim
		val old = el.getAttribute("old-title")

		el.removeAttribute("contenteditable")
		el.removeAttribute("old-title")

		if (title matches """^\s*$""") {
			el.textContent = old
		} else if (title != old) {
			Http.post(s"/composer/${ fragment.doc }/${ fragment.id }/rename", title)
		}
	}

	/** Deletes a fragment */
	private def deleteFragment(fragment: Fragment): Unit = {
		Http.delete(s"/composer/${ fragment.doc }/${ fragment.id }")
	}

	private val blocks = fragments.map(frags => frags.map(fragmentBlock))

	mount("#composer-main")(
		<div class="fragments-list">
			{blocks}
		</div>,
		<div class="new">
			<h3 class="gray">Nouvelle section</h3>
			<div class="row">
				<button class="gray alternate" onclick={() => createFragment("text")}>
					<i>text_fields</i>
					Texte
				</button>
				<button class="gray alternate" onclick={() => createFragment("group")}>
					<i>people</i>
					Groupe
				</button>
				<button class="gray alternate" onclick={() => createFragment("grid")}>
					<i>border_all</i>
					Grid
				</button>
			</div>
		</div>
	)

	/** Creates a new fragment */
	private def createFragment(style: String): Unit = {
		Http.post(s"/composer/$doc/create", Json.obj("style" -> style))
	}

	def receive: Receive = {
		case RefreshEventKey ~ _ => refreshFragments()
	}
}
