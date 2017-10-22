package gt.util

import mhtml.{Cancelable, Rx, _}
import org.scalajs.dom
import org.scalajs.dom.ext.EasySeq
import org.scalajs.dom.html
import play.api.libs.json.{Json, Reads}
import scala.scalajs.js.URIUtils
import scala.xml.{Atom, Group, Node}

trait ViewUtils {
	protected def localStorage: dom.ext.Storage = dom.ext.LocalStorage

	protected def $[E <: html.Element](selector: String): E = $(dom.document, selector)
	protected def $[E <: html.Element](parent: dom.NodeSelector, selector: String): E = {
		parent.querySelector(selector).asInstanceOf[E]
	}

	protected implicit def PimpedNodeListOf[N <: dom.Node](nodes: dom.NodeListOf[N]): EasySeq[N] =
		new EasySeq[N](nodes.length, nodes.apply)

	protected def $$[E <: html.Element](selector: String): dom.NodeListOf[E] = $$(dom.document, selector)
	protected def $$[E <: html.Element](parent: dom.NodeSelector, selector: String): dom.NodeListOf[E] = {
		parent.querySelectorAll(selector).asInstanceOf[dom.NodeListOf[E]]
	}

	private val mountSettings = new ProdSettings {}

	def mount(selector: String)(children: Node*): Cancelable = {
		val node = if (children.size == 1) children.head else Group(children)
		mhtml.mount($[html.Element](selector), node, mountSettings)
	}

	def mount(selector: String, obs: Rx[Node]): Cancelable = {
		mhtml.mount($[html.Element](selector), new Atom(obs), mountSettings)
	}

	def value[T: Reads](key: String, default: => T = throw new NoSuchElementException("Non-existing value")): T = {
		Option(dom.document.querySelector(s"script[type='application/gt-value'][key='$key']"))
			.map(_.textContent)
			.map(_.replace('+', ' '))
			.map(URIUtils.decodeURIComponent)
			.map(Json.parse)
			.flatMap(_.asOpt[T])
			.getOrElse(default)
	}
}
