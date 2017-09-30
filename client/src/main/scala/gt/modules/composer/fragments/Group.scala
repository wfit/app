package gt.modules.composer.fragments

import models.composer.Fragment
import org.scalajs.dom
import org.scalajs.dom.DragEvent

case class Group (fragment: Fragment) extends FragmentTree {

	val tree = {
		<div class="group-fragment">
			<div class="fake-group" ondragover={e: dom.DragEvent => dragOver(e)}></div>
		</div>
	}

	def dragOver(e: DragEvent): Unit = {
		e.preventDefault()
		e.dataTransfer.dropEffect = "copy"
	}
}
