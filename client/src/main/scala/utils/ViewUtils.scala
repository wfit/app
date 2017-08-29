package utils

import org.scalajs.dom
import org.scalajs.dom.html
import scala.concurrent.ExecutionContext

trait ViewUtils {
	protected implicit val ec: ExecutionContext = ExecutionContext.global

	protected def localStorage: dom.ext.Storage = dom.ext.LocalStorage

	protected def $[E <: html.Element](selector: String): E = $(dom.document, selector)
	protected def $[E <: html.Element](parent: dom.NodeSelector, selector: String): E = {
		parent.querySelector(selector).asInstanceOf[E]
	}
}
