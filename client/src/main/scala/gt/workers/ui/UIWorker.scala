package gt.workers.ui

import gt.workers.{AutoWorker, Worker}
import gt.workers.ui.UIWorker.Notification
import org.scalajs.dom
import play.api.libs.json.{Format, Json}
import protocol.MessageSerializer
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global, literal, newInstance}
import scala.scalajs.js.annotation.JSExportTopLevel

class UIWorker extends Worker {
	def receive: Receive = {
		case 'LocalStorageGet ~ (key: String) =>
			sender ! dom.window.localStorage.getItem(key)

		case 'LocalStorageSet ~ (key: String) ~ null =>
			dom.window.localStorage.removeItem(key)

		case 'LocalStorageSet ~ (key: String) ~ (value: String) =>
			dom.window.localStorage.setItem(key, value)

		case Notification(title, body, silent) =>
			val notification = newInstance(global.Notification)(title, literal(
				body = body,
				silent = silent,
				icon = "/assets/images/wfit.png",
			))

		case other =>
			dom.console.warn("UI Worker received message: ", other.asInstanceOf[js.Any])
	}
}

object UIWorker extends AutoWorker.Named[UIWorker]("ui") {
	case class Notification(title: String, body: String, silent: Boolean = false)

	implicit val NotificationFormat: Format[Notification] = Json.format[Notification]
	implicit object NotificationSerializer extends MessageSerializer.Json(NotificationFormat)

	@JSExportTopLevel("test_notify")
	def test(): Unit = {
		UIWorker.ref ! Notification("Foo", "bar")
	}
}
