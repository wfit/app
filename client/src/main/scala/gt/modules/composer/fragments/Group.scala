package gt.modules.composer.fragments

import gt.modules.composer.Composer
import gt.util.Http
import java.util.concurrent.atomic.AtomicInteger
import mhtml.{Rx, Var}
import models.Toon
import models.composer.{Fragment, Slot}
import org.scalajs.dom
import org.scalajs.dom.{html, DragEvent}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import utils.UUID
import utils.JsonFormats._

case class Group (fragment: Fragment) extends FragmentTree {
	private val slots = Var(Seq.empty[(Slot, Option[Toon])])

	val members: Rx[Set[UUID]] = slots.map { ss =>
		ss.collect { case (_, Some(toon)) => toon.uuid }.toSet
	}

	val ownersCount: Rx[Map[UUID, Int]] = slots.map { ss =>
		ss.collect { case (_, Some(toon)) => toon.owner }.groupBy(identity).map { case (k, v) => (k, v.size) }
	}

	private val tiers = slots.map { ss =>
		ss.groupBy { case (s, _) => s.row }
	}

	private val maxTier = tiers.map { ts =>
		if (ts.isEmpty) -1 else ts.keys.max
	}

	private val fakeTier = maxTier.map(_ + 1)

	private val slotsByTier = (tiers product maxTier).map { case (ts, max) =>
		(0 to max).map(idx => ts.getOrElse(idx, Seq.empty).sorted(Composer.StandardSlotToonOrdering)).zipWithIndex
	}

	private def slotsTree(slots: Seq[(Slot, Option[Toon])]) = slots.map { case (slot, toon) =>
		<span class="toon" wow-class={slot.cls.id.toString} draggable="true"
		      duplicate={ownersCount.map(oc => toon.flatMap(t => oc.get(t.owner)).getOrElse(1) > 1)}
		      ondragstart={e: dom.DragEvent => dragStart(e, slot)}
		      ondragend={e: dom.DragEvent => dragEnd()}>
			{slot.name}
		</span>
	}

	private def tierTree(tier: Int, slots: Seq[Elem] = Seq.empty, cls: String = "tier") = {
		val counter = new AtomicInteger(0)
		<div class={cls}
		     ondragenter={e: dom.DragEvent => dragEnter(e, counter, tier)}
		     ondragleave={e: dom.DragEvent => dragLeave(e, counter)}
		     ondragover={e: dom.DragEvent => dragOver(e, tier)}
		     ondrop={e: dom.DragEvent => dragDrop(e, counter, tier)}>
			{slots}
		</div>
	}

	val tiersTree = (slotsByTier product fakeTier).map { case (ts, fakeId) =>
		ts.map { case (ss, tier) =>
			tierTree(tier, slotsTree(ss))
		} :+ tierTree(fakeId, cls = "tier fake")
	}

	val tree = {
		<div class="group-fragment">
			{tiersTree}
		</div>
	}

	def dragStart(e: DragEvent, slot: Slot): Unit = {
		Composer.dragType = "slot"
		Composer.dragSlot = slot
	}

	private def dragEnd(): Unit = {
		Composer.dragType = null
		Composer.dragSlot = null
	}

	private def acceptableSlot(tier: Int): Boolean = {
		Composer.dragType == "slot" && (Composer.dragSlot.fragment != fragment.id || Composer.dragSlot.row != tier)
	}

	private def acceptableDrag(tier: Int): Boolean = {
		Composer.dragType == "toon" || acceptableSlot(tier)
	}

	private def dragEnter(e: dom.DragEvent, counter: AtomicInteger, tier: Int): Unit = {
		counter.incrementAndGet()
		val el = e.currentTarget.asInstanceOf[html.Element]
		if (acceptableDrag(tier)) {
			e.preventDefault()
			el.classList.add("hover")
		}
	}

	private def dragLeave(e: dom.DragEvent, counter: AtomicInteger, drop: Boolean = false): Unit = {
		if (drop || counter.decrementAndGet() <= 0) {
			val el = e.currentTarget.asInstanceOf[html.Element]
			el.classList.remove("hover")
			counter.set(0)
		}
	}

	private def dragOver(e: dom.DragEvent, tier: Int): Unit = {
		if (acceptableDrag(tier)) {
			e.preventDefault()
			e.dataTransfer.dropEffect = Composer.dragType match {
				case "slot" if !e.ctrlKey || Composer.dragSlot.fragment == fragment.id => "move"
				case _ => "copy"
			}
		}
	}

	private def dragDrop(e: dom.DragEvent, counter: AtomicInteger, tier: Int): Unit = {
		if (acceptableDrag(tier)) {
			e.preventDefault()
			Http.post(s"/composer/${ fragment.doc }/${ fragment.id }/slots", Composer.dragType match {
				case "toon" => Json.obj("toon" -> Composer.dragToon, "row" -> tier)
				case "slot" => Json.obj("slot" -> Composer.dragSlot.id, "row" -> tier, "copy" -> e.ctrlKey)
			})
		}
		dragLeave(e, counter, drop = true)
	}

	def refresh(): Unit = {
		for (res <- Http.get(s"/composer/${ fragment.doc }/${ fragment.id }/slots")) {
			slots := res.json.as[Seq[(Slot, Option[Toon])]]
		}
	}
}
