package gt.modules.composer.sidebar

import mhtml.Rx
import scala.xml.Node

class Dummy extends SidebarTree {
	val tree: Rx[Node] = Rx { <!-- Placeholder --> }
	def refresh(): Unit = ()
}
