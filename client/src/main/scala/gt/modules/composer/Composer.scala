package gt.modules.composer

import gt.Router
import gt.util.{Http, View}
import gt.workers.Worker
import org.scalajs.dom.html

class Composer extends Worker.Dummy with View {
	for (doc <- $$[html.Div]("#composer-docs .doc")) {
		doc.onclick = { _ =>
			$$[html.Element]("#composer-docs .focused").foreach(_.classList.remove("focused"))
			doc.classList.add("focused")

			val id = doc.getAttribute("data-id")
			Http.get(Router.Composer.document(id)).map { res =>
				$[html.Element]("#composer-view").innerHTML = res.text
			}
		}
	}
}
