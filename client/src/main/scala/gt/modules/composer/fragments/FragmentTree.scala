package gt.modules.composer.fragments

import mhtml.Rx
import models.composer.Fragment
import scala.xml.Elem
import utils.UUID

trait FragmentTree {
	val members: Rx[Set[UUID]]

	val fragment: Fragment
	val tree: Elem

	def refresh(): Unit
}
