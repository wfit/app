package gt.modules.composer.fragments

import mhtml.Rx
import models.UUID
import models.composer.Fragment
import scala.xml.Elem

trait FragmentTree {
	val members: Rx[Set[UUID]]

	val fragment: Fragment
	val settings: Elem = <div></div>
	val tree: Elem

	def refresh(): Unit
}
