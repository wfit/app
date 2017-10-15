package gt.modules.composer.fragments

import gt.Router
import gt.modules.composer.{ComposerUtils, Editor}
import gt.util.Http
import java.util.concurrent.atomic.AtomicInteger
import mhtml.{Rx, Var}
import models.Toon
import models.composer.{Fragment, Slot}
import models.wow.Relic
import org.scalajs.dom
import org.scalajs.dom.{DragEvent, html}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import utils.JsonFormats._
import utils.UUID

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
		(0 to max).map(idx => ts.getOrElse(idx, Seq.empty).sorted(ComposerUtils.StandardSlotToonOrdering)).zipWithIndex
	}

	private def relicList(relics: (Relic, Relic, Relic)): String = relics match {
		case (a, b, c) => Seq(a.name, b.name, c.name).mkString(" / ")
	}

	private def artifactInfo(toon: Toon) = {
		<div class="spacer">
			<div class="line gray">
				<span>{toon.spec.artifact.name}</span>
			</div>
			<div class="line">
				<span>{relicList(toon.spec.artifact.relics)}</span>
			</div>
		</div>
	}

	private def slotsTree(slots: Seq[(Slot, Option[Toon])]) = slots.map { case (slot, toon) =>
		<span class="toon" wow-class={slot.cls.id.toString} draggable="true" tooltip="1"
		      duplicate={ownersCount.map(oc => toon.flatMap(t => oc.get(t.owner)).getOrElse(1) > 1)}
		      ondragstart={e: dom.DragEvent => dragStart(e, slot)}
		      ondragend={e: dom.DragEvent => dragEnd()}>
			{toon.map(_.localName) getOrElse slot.name}
			<div class="tooltip">
				<div class="line">
					<span wow-class={slot.cls.id.toString}>
						{toon.map(_.fullName) getOrElse slot.name}
					</span>
					<span>
						{toon.map(_.ilvl.toString)} ilvl
					</span>
				</div>
				{toon.map(artifactInfo)}
			</div>
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

	private val stats = slots.map(ComposerUtils.computeAllStats)

	private def renderStat(data: (Option[String], Int, Double)) = data match {
		case (icon, count, ilvl) =>
			<span>
				{icon.map(src => <img src={src}/>)}
				<strong>{count}</strong>
				<span class="gray">({ilvl})</span>
			</span>
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
			Http.post(Router.Composer.setSlot(fragment.doc, fragment.id), Editor.dragType match {
				case "toon" => Json.obj("toon" -> Editor.dragToon, "row" -> tier)
				case "slot" => Json.obj("slot" -> Editor.dragSlot.id, "row" -> tier, "copy" -> e.ctrlKey)
			})
		}
		dragLeave(e, counter, drop = true)
	}

	def refresh(): Unit = {
		for (res <- Http.get(Router.Composer.getSlots(fragment.doc, fragment.id))) {
			slots := res.json.as[Seq[(Slot, Option[Toon])]]
		}
	}
}
