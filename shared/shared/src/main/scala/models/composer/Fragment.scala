package models.composer

import models.composer.Fragment.Style
import utils.UUID

case class Fragment (id: UUID, doc: UUID, sort: Int, style: Style, title: String)


object Fragment {
	sealed abstract class Style (val value: String)
	case object Text extends Style("text")
	case object Group extends Style("group")
	case object Grid extends Style("grid")

	def styleFromString(style: String): Style = style match {
		case "text" => Text
		case "group" => Group
		case "grid" => Grid
		case other => throw new NoSuchElementException(other)
	}
}
