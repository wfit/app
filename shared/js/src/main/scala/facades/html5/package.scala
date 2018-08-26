package facades

import org.scalajs.dom
import org.scalajs.dom.html

package object html5 {
  @inline implicit def Element(element: dom.Element): html5.Element          = element.asInstanceOf[html5.Element]
  @inline implicit def HTMLElement(element: html.Element): html5.HTMLElement = element.asInstanceOf[html5.HTMLElement]
}
