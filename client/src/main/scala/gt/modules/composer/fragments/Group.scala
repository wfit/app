package gt.modules.composer.fragments

import gt.modules.composer.Editor
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
		(0 to max).map(idx => ts.getOrElse(idx, Seq.empty).sorted(Editor.StandardSlotToonOrdering)).zipWithIndex
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
			<div class="tiers">
				{tiersTree}
			</div>
		</div>
	}

	private def roundIlvl(sum: Int, count: Int): Double = {
		if (count < 1) 0
		else (sum.toDouble / count + 0.5).floor
	}

	private def computeStats(members: Seq[(Slot, Option[Toon])]): (Int, Double) = {
		val count = members.size
		val ilvlSum = members.flatMap { case (_, t) => t }.map(_.ilvl).sum
		(count, roundIlvl(ilvlSum, count))
	}

	private val stats = slots.map { ss =>
		val (count, ilvl) = computeStats(ss)
		val subStats = ss.groupBy { case (s, _) => s.role }.mapValues(computeStats)
			.toSeq.sortBy(_._1)
			.map { case (role, (c, i)) => (Some(role.icon), c, i) }
		(None, count, ilvl) +: subStats
	}

	private def renderStat(data: (Option[String], Int, Double)) = data match {
		case (icon, count, ilvl) =>
			<span>{icon.map(src => <img src={src} />)}<strong>{count}</strong> <span class="gray">({ilvl})</span></span>
	}

	override val settings: Elem = {
		<div class="stats">
			{stats.map(_.map(renderStat))}
		</div>
	}

	def dragStart(e: DragEvent, slot: Slot): Unit = {
		Editor.dragType = "slot"
		Editor.dragSlot = slot
	}

	private def dragEnd(): Unit = {
		Editor.dragType = null
		Editor.dragSlot = null
	}

	private def acceptableSlot(tier: Int): Boolean = {
		Editor.dragType == "slot" && (Editor.dragSlot.fragment != fragment.id || Editor.dragSlot.row != tier)
	}

	private def acceptableDrag(tier: Int): Boolean = {
		Editor.dragType == "toon" || acceptableSlot(tier)
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
			e.dataTransfer.dropEffect = Editor.dragType match {
				case "slot" if !e.ctrlKey || Editor.dragSlot.fragment == fragment.id => "move"
				case _ => "copy"
			}
		}
	}

	private def dragDrop(e: dom.DragEvent, counter: AtomicInteger, tier: Int): Unit = {
		if (acceptableDrag(tier)) {
			e.preventDefault()
			Http.post(s"/composer/${ fragment.doc }/${ fragment.id }/slots", Editor.dragType match {
				case "toon" => Json.obj("toon" -> Editor.dragToon, "row" -> tier)
				case "slot" => Json.obj("slot" -> Editor.dragSlot.id, "row" -> tier, "copy" -> e.ctrlKey)
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
