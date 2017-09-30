package gt.modules.composer.fragments

import models.composer.Fragment

case class Text (fragment: Fragment) extends FragmentTree {
	val tree = {
		<div class="text-fragment">
		</div>
	}
}
