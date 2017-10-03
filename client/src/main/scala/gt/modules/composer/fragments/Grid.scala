package gt.modules.composer.fragments

import mhtml.Rx
import models.Toon
import models.composer.Fragment

case class Grid (fragment: Fragment) extends FragmentTree {
	val members: Rx[Set[Toon]] = Rx(Set.empty)

	val tree = {
		<div class="text-fragment">
		</div>
	}
	def refresh(): Unit = ()
}
