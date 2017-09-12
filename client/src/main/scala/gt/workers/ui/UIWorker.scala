package gt.workers.ui

import gt.workers.{AutoWorker, Worker}
import org.scalajs.dom
import scala.scalajs.js

class UIWorker extends Worker {
	def receive: Receive = {
		case 'LocalStorageGet ~ (key: String) =>
			sender ! dom.window.localStorage.getItem(key)
		case other =>
			dom.console.warn("UI Worker received message: ", other.asInstanceOf[js.Any])
	}
}

object UIWorker extends AutoWorker.Named[UIWorker]("ui")
