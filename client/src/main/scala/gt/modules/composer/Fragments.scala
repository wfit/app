package gt.modules.composer

import gt.util.{Http, ViewUtils}
import gt.workers.Worker
import gt.workers.eventbus.EventBus
import mhtml.Var
import models.composer.Fragment
import org.scalajs.dom
import org.scalajs.dom.html
import play.api.libs.json.Json
import scala.xml.Elem
import utils.UUID

class Fragments extends Worker with ViewUtils {
	val doc = value[String]("document-id")

	val RefreshEventKey = s"composer:$doc:fragments.refresh"
	EventBus.subscribe(RefreshEventKey)

	val fragments = Var(Seq.empty[Fragment])
	refreshFragments()

	def refreshFragments(): Unit = {
		for (res <- Http.get(s"/composer/$doc/fragments")) {
			fragments := res.json.as[Seq[Fragment]]
		}
	}

	private def fragmentBlock(fragment: Fragment): Elem = {
		<div class="fragment"
		     ondragover={(e: dom.DragEvent) => fragmentDragOver(e, fragment.id)}
		     ondragleave={(e: dom.DragEvent) => fragmentDragLeave(e)}
		     ondrop={(e: dom.DragEvent) => fragmentDragDrop(e, fragment.id)}>
			<h3 draggable="true"
			    ondragstart={(e: dom.DragEvent) => fragmentDragStart(e, fragment.id)}
			    ondragend={() => fragmentDragEnd()}>
				<i>
					{fragmentIcon(fragment.style)}
				</i>{fragment.title}
			</h3>
		</div>
	}

	private def fragmentIcon(style: Fragment.Style): String = style match {
		case Fragment.Text => "text_fields"
		case Fragment.Group => "people"
		case Fragment.Grid => "border_all"
	}

	private def fragmentDragStart(event: dom.DragEvent, id: UUID): Unit = {
		Composer.dragType = "fragment"
		Composer.dragFragment = id
		event.dataTransfer.dropEffect = "move"
	}

	private def fragmentDragEnd(): Unit = {
		Composer.dragType = null
	}

	private def fragmentDragOver(event: dom.DragEvent, id: UUID): Unit = {
		if (Composer.dragType == "fragment" && Composer.dragFragment != id) {
			event.preventDefault()
			val el = event.currentTarget.asInstanceOf[html.Element]
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

	private def fragmentDragLeave(event: dom.DragEvent): Unit = {
		val el = event.currentTarget.asInstanceOf[html.Element]
		el.classList.remove("drag-before")
		el.classList.remove("drag-after")
	}

	private def fragmentDragDrop(event: dom.DragEvent, id: UUID): Unit = {
		if (Composer.dragType == "fragment" && Composer.dragFragment != id) {
			val el = event.currentTarget.asInstanceOf[html.Element]
			el.classList.remove("drag-before")
			el.classList.remove("drag-after")

			val rect = el.getBoundingClientRect()
			Http.post(s"/composer/$doc/move", Json.obj(
				"source" -> Composer.dragFragment,
				"target" -> id,
				"position" -> (if (event.clientY - rect.top < rect.height / 2) "before" else "after")
			))
		}
	}

	private val blocks = fragments.map(frags => frags.map(fragmentBlock))

	mount("#composer-main .fragments-mount") {
		<div>
			{blocks}
		</div>
	}

	def receive: Receive = {
		case RefreshEventKey ~ _ => refreshFragments()
	}
}
