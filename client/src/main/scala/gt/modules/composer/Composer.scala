package gt.modules.composer

import gt.util.{Http, ViewUtils, WorkerView}
import gt.workers.Worker
import org.scalajs.dom.html
import play.api.libs.json.Json

class Composer extends Worker with ViewUtils {
	val doc = value[String]("document-id")
	val sidebar = Worker.local[Sidebar]

	// Fragment creators
	for (btn <- $$[html.Button](".fragment.new button")) {
		btn.onclick = { _ => createFragment(btn.getAttribute("data-type")) }
	}

	def receive: Receive = {
		case None => ???
	}

	def createFragment(style: String): Unit = {
		Http.post(s"/composer/$doc/create", Json.obj("style" -> style))
	}
}

object Composer extends WorkerView[Composer]
