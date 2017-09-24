package gt.modules.composer

import gt.util.{Http, ViewUtils, WorkerView}
import gt.workers.Worker
import org.scalajs.dom.html
import play.api.libs.json.Json
import utils.UUID

class Composer extends Worker with ViewUtils {
	val doc = value[String]("document-id")

	val sidebar = Worker.local[Sidebar]
	val fragments = Worker.local[Fragments]

	// Fragment creators
	for (btn <- $$[html.Button](".fragment.new button")) {
		btn.onclick = { _ => createFragment(btn.getAttribute("data-type")) }
	}

	def createFragment(style: String): Unit = {
		Http.post(s"/composer/$doc/create", Json.obj("style" -> style))
	}

	def receive: Receive = {
		case None => ???
	}

	override def onTerminate(): Unit = {
		sidebar.terminate()
		fragments.terminate()
	}
}

object Composer extends WorkerView[Composer] {
	var dragType = ""
	var dragFragment = UUID.zero
}
