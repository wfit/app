package gt.modules.composer.fragments

import mhtml.Rx
import models.Toon
import models.composer.Fragment
import scala.xml.Elem

trait FragmentTree {
	val members: Rx[Set[Toon]]

	val fragment: Fragment
	val tree: Elem

	def refresh(): Unit
}
