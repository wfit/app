package gt.modules.settings

import gt.{Settings => Registry}
import gt.util.View
import gt.Settings.SettingApi
import gt.workers.ui.UIWorker
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.ext._

class Settings extends View {
	def findSettingForKey(key: String) = Registry.settings(key)

	for (node <- dom.document.querySelectorAll("input[type=checkbox][data-key]");
	     input = node.asInstanceOf[html.Input]) {
		val key = input.getAttribute("data-key")
		val setting = findSettingForKey(key).asInstanceOf[SettingApi[Boolean]]
		input.checked = setting.get
		input.onclick = { _: dom.MouseEvent =>
			setting := input.checked
		}
	}

	dom.document.getElementById("notification-test").addEventListener("click", (_: dom.MouseEvent) => {
		UIWorker.ref ! UIWorker.Notification(
			title = "Notification de test",
			body = "Hello, world!"
		)
	})
}
