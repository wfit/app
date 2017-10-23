package gt.util

import org.scalajs.dom
import org.scalajs.dom.ext.EasySeq
import org.scalajs.dom.html
import play.api.libs.json.{Json, Reads}
import scala.scalajs.js.URIUtils

trait ViewUtils {
	/** Direct access to local storage */
	protected def localStorage: dom.ext.Storage = dom.ext.LocalStorage

	/** Queries an element by selector */
	protected def $[E <: html.Element](selector: String): E = $(dom.document, selector)

	/** Queries an element by selector inside a given parent */
	protected def $[E <: html.Element](parent: dom.NodeSelector, selector: String): E = {
		parent.querySelector(selector).asInstanceOf[E]
	}

	/** Allows treating NodeListOf as Scala Seq */
	protected implicit def PimpedNodeListOf[N <: dom.Node](nodes: dom.NodeListOf[N]): EasySeq[N] =
		new EasySeq[N](nodes.length, nodes.apply)

	/** Queries multiples elements by selector */
	protected def $$[E <: html.Element](selector: String): dom.NodeListOf[E] = $$(dom.document, selector)

	/** Queries multiples element by selector inside a given parent */
	protected def $$[E <: html.Element](parent: dom.NodeSelector, selector: String): dom.NodeListOf[E] = {
		parent.querySelectorAll(selector).asInstanceOf[dom.NodeListOf[E]]
	}

	/** Default value for the [[value]] method */
	private def defaultValue: Nothing = throw new NoSuchElementException("Non-existing value")

	/** Access a view-provided value */
	protected def value[T: Reads](key: String, default: => T = defaultValue): T = {
		Option(dom.document.querySelector(s"script[type='application/gt-value'][key='$key']"))
			.map(_.textContent)
			.map(_.replace('+', ' '))
			.map(URIUtils.decodeURIComponent)
			.map(Json.parse)
			.flatMap(_.asOpt[T])
			.getOrElse(default)
	}
}
