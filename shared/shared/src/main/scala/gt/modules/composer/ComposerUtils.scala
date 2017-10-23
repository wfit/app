package gt.modules.composer

import models.Toon
import models.composer.Slot
import models.wow.Role

object ComposerUtils {
	def roundIlvl(sum: Int, count: Int): Double = {
		if (count < 1) 0
		else (sum.toDouble / count + 0.5).floor
	}

	def computeStats(members: Seq[(Slot, Option[Toon])]): (Int, Double) = {
		val count = members.size
		val ilvlSum = members.flatMap { case (_, t) => t }.map(_.ilvl).sum
		(count, roundIlvl(ilvlSum, count))
	}

	def computeAllStats(members: Seq[(Slot, Option[Toon])]): Seq[(Option[String], Int, Double)] = {
		val (count, ilvl) = computeStats(members)
		val subStats = members.groupBy { case (s, _) => s.role }.mapValues(computeStats)
			.toSeq.sortBy(_._1)
			.map { case (role, (c, i)) => (Some(role.icon), c, i) }
		(None, count, ilvl) +: subStats
	}

	val StandardOrdering: Ordering[(Boolean, Role, Int, String)] =
		Ordering.by { case (main, role, ilvl, name) => (!main, role, -ilvl, name) }

	val StandardToonOrdering: Ordering[Toon] =
		StandardOrdering.on(t => (t.main, t.spec.role, t.ilvl, t.name))

	val StandardSlotToonOrdering: Ordering[(Slot, Option[Toon])] = {
		StandardOrdering.on { case (slot, toon) =>
			(toon.exists(_.main), slot.role, toon.map(_.ilvl) getOrElse 0, slot.name)
		}
	}
}
