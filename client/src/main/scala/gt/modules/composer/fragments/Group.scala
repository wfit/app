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

/**
  * A group is a structure of 5 tiers in which toons can be placed.
  * This is the main unit of composition for raid groups.
  */
case class Group (fragment: Fragment) extends FragmentTree {
	/** A cache of every slot entries for this group,
	  * this value if updated on every refresh from the server */
	private val slots = Var(Seq.empty[(Slot, Option[Toon])])

	/** The set of every toons in this fragment */
	val members: Rx[Set[UUID]] = slots.map { ss =>
		ss.collect { case (_, Some(toon)) => toon.uuid }.toSet
	}

	/** The number of toons for every distinct owner,
	  * used to compute conflicts*/
	val ownersCount: Rx[Map[UUID, Int]] = slots.map { ss =>
		ss.collect { case (_, Some(toon)) => toon.owner }.groupBy(identity).map { case (k, v) => (k, v.size) }
	}

	/** The set of slots splits into tiers */
	private val tiers = slots.map { ss =>
		ss.groupBy { case (s, _) => s.row }
	}

	/** The set of slots for each tiers,
	  * it differs from [[tiers]] by also including empty tiers and having
	  * the tier index zipped with the sequence of slots */
	private val slotsByTier = tiers.map { ts =>
		(0 to 4).map(idx => ts.getOrElse(idx, Seq.empty).sorted(ComposerUtils.StandardSlotToonOrdering)).zipWithIndex
	}

	/** Formats the tuple of relics from an artifact into a string value */
	private def relicList(relics: (Relic, Relic, Relic)): String = relics match {
		case (a, b, c) => Seq(a.name, b.name, c.name).mkString(" / ")
	}

	/** Builds the artifact info DOM tree for the given toon */
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

	/** Builds the DOM tree for a given slot */
	private def slotsTree(slots: Seq[(Slot, Option[Toon])]) = slots.map { case (slot, toon) =>
		<span class="toon" wow-class={slot.cls.id.toString}
		      main={toon.exists(_.main)}
		      duplicate={ownersCount.map(oc => toon.flatMap(t => oc.get(t.owner)).getOrElse(1) > 1)}
		      draggable="true" tooltip="1"
		      ondragstart={e: dom.DragEvent => dragStart(e, slot)}
		      ondragend={() => dragEnd()}>
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

	/** Builds the DOM tree for a given tier */
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

	/** The DOM tree for each tiers */
	private val tiersTree = slotsByTier.map { ts =>
		ts.map { case (ss, tier) =>
			tierTree(tier, slotsTree(ss))
		}
	}

	/** The complete DOM tree for this fragment */
	val tree = {
		<div class="group-fragment">
			<div class="tiers">
				{tiersTree}
			</div>
		</div>
	}

	/** Filters out non-main from stats if such filter is enabled */
	private val statsSlots = (slots product Editor.filterMains).map {
		case (ss, false) => ss
		case (ss, true) => ss.filter { case (_, toon) => toon.exists(_.main) }
	}

	/** Composition stats: counts and average ilvl */
	private val stats = statsSlots.map(ComposerUtils.computeAllStats)

	private def renderStat(data: (Option[String], Int, Double)) = data match {
		case (icon, count, ilvl) =>
			<span>
				{icon.map(src => <img src={src}/>)}
				<strong>{count}</strong>
				<span class="gray">({ilvl})</span>
			</span>
	}

	/** Use the settings display to show stats */
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

	/** Whether the slot is an acceptable drag,
	  * this is the case if the slot is not from the same tier */
	private def acceptableSlot(tier: Int): Boolean = {
		Editor.dragType == "slot" && (Editor.dragSlot.fragment != fragment.id || Editor.dragSlot.row != tier)
	}

	/** Whether the drag object is acceptable for a drop in a tier zone */
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

	/** Refreshes slots for this group from the server */
	def refresh(): Unit = {
		for (res <- Http.get(Router.Composer.getSlots(fragment.doc, fragment.id))) {
			slots := res.json.as[Seq[(Slot, Option[Toon])]]
		}
	}
}
