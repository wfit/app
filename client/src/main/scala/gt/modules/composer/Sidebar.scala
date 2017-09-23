package gt.modules.composer

import gt.util.ViewUtils
import gt.workers.Worker
import mhtml.{Rx, Var}

class Sidebar extends Worker with ViewUtils {
	val doc = value[String]("document-id")

	Roster.refresh()

	val tab = Var("none")
	selectTab("roster")

	def selectTab(selection: String): Unit = {
		tab := selection
		selection match {
			case "roster" => Roster.refresh()
			case _ => // ignore
		}
	}

	def tabSelected(key: String): Rx[Boolean] = tab.map(_ == key)

	private def tabs(items: (String, String)*) = items.map { case (key, icon) =>
		<li onclick={() => selectTab(key)} selected={tabSelected(key)}>
			<i>
				{icon}
			</i>
		</li>
	}

	mount("#composer-sidebar .tabs-container") {
		<ul class="tabs">{tabs(
			"roster" -> "people",
			"slacks" -> "flash_on",
			"wishes" -> "star",
			"check" -> "lightbulb_outline"
		)}</ul>
	}

	val tree = tab.map {
		case "roster" => Roster.tree
		case _ => <!-- Nothing -->
	}

	val tabBinding = mount("#composer-sidebar .content", tree)

	def receive: Receive = {
		case _ => ???
	}

	override def onTerminate(): Unit = {
		tabBinding.cancel
	}
}
