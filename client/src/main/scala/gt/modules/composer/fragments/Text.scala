package gt.modules.composer.fragments

import mhtml.Rx
import models.UUID
import models.composer.Fragment

case class Text (fragment: Fragment) extends FragmentTree {
	val members: Rx[Set[UUID]] = Rx(Set.empty)

	val tree = {
		<div class="text-fragment">
		</div>
	}
	def refresh(): Unit = ()
}
