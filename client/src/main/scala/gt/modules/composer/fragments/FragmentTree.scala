package gt.modules.composer.fragments

import models.composer.Fragment
import scala.xml.Elem

trait FragmentTree {
	val fragment: Fragment
	val tree: Elem
}
