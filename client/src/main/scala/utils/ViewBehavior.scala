package utils

import org.scalajs.dom
import org.scalajs.dom.html

trait ViewBehavior {
	def localStorage: dom.ext.Storage = dom.ext.LocalStorage
	def $[E <: html.Element](selector: String): E = dom.document.querySelector(selector).asInstanceOf[E]
}
