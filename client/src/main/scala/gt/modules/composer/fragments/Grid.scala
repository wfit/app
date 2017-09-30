package gt.modules.composer.fragments

import models.composer.Fragment

case class Grid (fragment: Fragment) extends FragmentTree {
	val tree = {
		<div class="text-fragment">
		</div>
	}
}
