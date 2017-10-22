package gt.modules.composer.sidebar

import mhtml.Rx
import scala.xml.Node

trait SidebarTree {
	val tree: Rx[Node]
	def refresh(): Unit
}
