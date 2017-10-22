package gt.modules.composer.sidebar

import gt.Router
import gt.modules.composer.{ComposerUtils, Editor, FragmentsList}
import gt.util.Http
import mhtml.{Rx, Var}
import models.composer.RosterEntry
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import utils.UUID

class Roster extends SidebarTree {
	private val roster = Var(Seq.empty[RosterEntry])

	private val rosterBuckets = roster.map { entries =>
		entries.groupBy {
			case entry if entry.rank < 2 && entry.toon.main => 'Friends
			case entry if entry.toon.main => 'Mains
			case _ => 'Alts
		}
	}

	private def rosterBucket(bucket: Symbol): Rx[Seq[RosterEntry]] = {
		rosterBuckets.map(_.getOrElse(bucket, Seq.empty))
	}

	private val rosterMains = rosterBucket('Mains)
	private val rosterFriends = rosterBucket('Friends)

	private val rosterAlts = rosterBucket('Alts).map { alts =>
		if (alts.isEmpty) Seq.empty
		else {
			val ilvlMax = alts.map(_.toon.ilvl).max
			val ceiling = ilvlMax - ilvlMax % 10
			val cutoffs = ceiling to (ceiling - 100) by -5
			val buckets = alts.groupBy(alt => cutoffs.find(_ < alt.toon.ilvl) getOrElse 0).toSeq.sortBy {
				case (ilvl, _) => -ilvl
			}
			coalesceAlts(buckets)
		}
	}

	private def ilvlRangeLabel(from: Int, to: Int): String = {
		(from, to) match {
			case (0, _) => "Crappy"
			case (a, 0) => s"$a+"
			case (a, b) => s"$a - $b"
		}
	}

	private def coalesceAlts(buckets: Seq[(Int, Seq[RosterEntry])]): Seq[(String, Seq[RosterEntry])] = {
		val seed = (0, Seq.empty[RosterEntry], Seq.empty[(String, Seq[RosterEntry])])
		val (lastIlvl, last, list) = buckets.foldLeft(seed) { case ((ceiling, previous, acc), (ilvl, group)) =>
			(previous.size, group.size) match {
				case (0, _) => (ceiling, group, acc)
				case (a, b) if a + b > 20 => (ilvl + 5, group, acc :+ (ilvlRangeLabel(ilvl + 5, ceiling), previous))
				case _ => (ceiling, previous ++ group, acc)
			}
		}
		list :+ (ilvlRangeLabel(0, lastIlvl), last)
	}

	private def toonsButtons(seq: Seq[RosterEntry]) = seq.sortBy(_.toon)(ComposerUtils.StandardToonOrdering).map { entry =>
		<span class="toon" wow-class={entry.toon.cls.id.toString} draggable="true" tooltip="true"
		      picked={FragmentsList.members.map(_ contains entry.toon.uuid)}
		      ondragstart={(e: dom.DragEvent) => toonDragStart(e, entry.toon.uuid)}
		      ondragend={() => toonDragEnd()}>
			{entry.toon.localName}
			<div class="tooltip">
				<div class="line">
					<span wow-class={entry.toon.cls.id.toString}>{entry.toon.fullName}</span>
					<span>{entry.toon.ilvl.toString} ilvl</span>
				</div>
				<div class="line">
					<span class="gray">{entry.owner.name}</span>
				</div>
			</div>
		</span>
	}

	private def toonDragStart(event: dom.DragEvent, uuid: UUID): Unit = {
		Editor.dragType = "toon"
		Editor.dragToon = uuid
	}

	private def toonDragEnd(): Unit = {
		Editor.dragType = null
		Editor.dragToon = UUID.zero
	}

	private def altsSections(sections: Seq[(String, Seq[RosterEntry])]) = sections.map { case (range, toons) =>
		<div>
			<h4 class="gray">
				{range}
			</h4>
			<div class="toons">
				{toonsButtons(toons)}
			</div>
		</div>
	}

	val tree = Rx {
		<div id="composer-sidebar-roster">
			<h3>Roster</h3>
			<div class="toons">
				{rosterMains.map(toonsButtons)}
			</div>
			<h3>Friends</h3>
			<div class="toons">
				{rosterFriends.map(toonsButtons)}
			</div>
			<h3>Alts</h3>
			<div>
				{rosterAlts.map(altsSections)}
			</div>
		</div>
	}

	def refresh(): Unit = {
		Http.get(Router.Composer.roster()).foreach { res =>
			roster := res.json.as[Seq[RosterEntry]]
		}
	}
}
