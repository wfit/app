package gt.tools

import mhtml.{Cancelable, Rx, _}
import org.scalajs.dom
import org.scalajs.dom.html
import scala.concurrent.ExecutionContext
import scala.xml.{Atom, Node}

trait ViewUtils {
	protected implicit val ec: ExecutionContext = ExecutionContext.global

	protected def localStorage: dom.ext.Storage = dom.ext.LocalStorage

	protected def $[E <: html.Element](selector: String): E = $(dom.document, selector)
	protected def $[E <: html.Element](parent: dom.NodeSelector, selector: String): E = {
		parent.querySelector(selector).asInstanceOf[E]
	}

	private val mountSettings = new ProdSettings {}

	def mount(selector: String)(child: Node): Cancelable = mhtml.mount($[html.Element](selector), child, mountSettings)
	def mount(selector: String, obs: Rx[Node]): Cancelable = mhtml.mount($[html.Element](selector), new Atom(obs), mountSettings)
}
