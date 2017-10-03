package gt.modules.composer.fragments

import gt.modules.composer.Composer
import gt.util.Http
import java.util.concurrent.atomic.AtomicInteger
import mhtml.{Rx, Var}
import models.Toon
import models.composer.{Fragment, GroupSlot}
import org.scalajs.dom
import org.scalajs.dom.html
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
import utils.JsonFormats._

case class Group (fragment: Fragment) extends FragmentTree {
	private val slots = Var(Seq.empty[(GroupSlot, Option[Toon])])

	val members: Rx[Set[Toon]] = slots.map { ss =>
		ss.collect { case (_, Some(toon)) => toon }.toSet
	}

	private val tiers = slots.map { ss =>
		ss.groupBy { case (s, _) => s.tier }
	}

	private val maxTier = tiers.map { ts =>
		if (ts.isEmpty) -1 else ts.keys.max
	}

	private val fakeTier = maxTier.map(_ + 1)

	private val slotsByTier = (tiers product maxTier).map { case (ts, max) =>
		(0 to max).map(idx => ts.getOrElse(idx, Seq.empty)).zipWithIndex
	}

	private def slotsTree(slots: Seq[(GroupSlot, Option[Toon])]) = slots.map { case (slot, toon) =>
		<span class="toon" wow-class={slot.cls.id.toString}>
			{slot.name}
		</span>
	}

	private def tierTree(tier: Int, slots: Seq[Elem] = Seq.empty, cls: String = "tier") = {
		val counter = new AtomicInteger(0)
		<div class={cls}
		     ondragenter={e: dom.DragEvent => dragEnter(e, counter)}
		     ondragleave={e: dom.DragEvent => dragLeave(e, counter)}
		     ondragover={e: dom.DragEvent => dragOver(e)}
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

	def dragEnter(e: dom.DragEvent, counter: AtomicInteger): Unit = {
		counter.incrementAndGet()
		val el = e.currentTarget.asInstanceOf[html.Element]
		if (Composer.dragType == "toon") {
			e.preventDefault()
			el.classList.add("hover")
		}
	}

	def dragLeave(e: dom.DragEvent, counter: AtomicInteger, drop: Boolean = false): Unit = {
		if (drop || counter.decrementAndGet() <= 0) {
			val el = e.currentTarget.asInstanceOf[html.Element]
			el.classList.remove("hover")
			counter.set(0)
		}
	}

	def dragOver(e: dom.DragEvent): Unit = {
		if (Composer.dragType == "toon") {
			e.preventDefault()
			e.dataTransfer.dropEffect = "copy"
		}
	}

	def dragDrop(e: dom.DragEvent, counter: AtomicInteger, tier: Int): Unit = {
		if (Composer.dragType == "toon") {
			e.preventDefault()
			Http.post(s"/composer/${ fragment.doc }/${ fragment.id }/slots",
				Json.obj("toon" -> Composer.dragToon, "tier" -> tier))
		}
		dragLeave(e, counter, drop = true)
	}

	def refresh(): Unit = {
		for (res <- Http.get(s"/composer/${ fragment.doc }/${ fragment.id }/slots")) {
			slots := res.json.as[Seq[(GroupSlot, Option[Toon])]]
		}
	}
}
