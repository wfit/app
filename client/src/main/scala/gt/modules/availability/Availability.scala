package gt.modules.availability

import facades.html5.HTMLElement
import gt.Router
import gt.util.{Http, View}
import java.time.{LocalDate, LocalTime}
import org.scalajs.dom.html.TableCell
import org.scalajs.dom.{MouseEvent, NodeListOf, html}
import play.api.libs.json.Json

class Availability extends View.Simple {
  private val table = $[html.Table]("#availability-table")

  final private val ClassAvailable  = "available"
  final private val ClassSelected   = "selected"
  final private val ClassUnselected = "unselected"

  for (availability <- value[Seq[models.availability.Availability]]("availabilities")) {
    val (day, start) = AvailabilityUtils.unpack(availability.from)
    val end          = availability.to.toLocalTime
    $[html.TableRow](table, s"tr[data-day='$day']") match {
      case null => // Ignore
      case row =>
        for (time <- AvailabilityUtils.timeRange(start, end)) {
          $[html.TableCell](row, s"td[data-time='$time']") match {
            case null => // Ignore
            case cell => cell.classList.add(ClassAvailable)
          }
        }
    }
  }

  private var dragging                 = false
  private var dragMode: String         = null
  private var dragStartDay: LocalDate  = null
  private var dragStartTime: LocalTime = null
  private var dragEndDay: LocalDate    = null
  private var dragEndTime: LocalTime   = null

  table.onmousedown = tableCellHandler { (cell, day, time) =>
    dragging = true
    dragStartDay = day
    dragStartTime = time
    dragEndDay = day
    dragEndTime = time
    dragMode = if (cell.classList.contains(ClassAvailable)) ClassUnselected else ClassSelected
    cell.classList.add(dragMode)
  }

  table.onmouseover = tableCellHandler { (_, day, time) =>
    if (dragging) {
      dragEndDay = day
      dragEndTime = time
      updateRectangle()
    }
  }

  table.onmouseup = tableCellHandler { (_, day, time) =>
    if (dragging) {
      dragging = false
      dragEndDay = day
      dragEndTime = time
      dragMode match {
        case ClassSelected   => rectangle.foreach(el => el.classList.add(ClassAvailable))
        case ClassUnselected => rectangle.foreach(el => el.classList.remove(ClassAvailable))
      }
      clearRectangle()
    }
  }

  private def rectangle: NodeListOf[TableCell] = $$[html.TableCell](table, s".$dragMode")

  private def clearRectangle(): Unit = rectangle.foreach(el => el.classList.remove(dragMode))

  private def updateRectangle(): Unit = {
    clearRectangle()
    for ((day, time) <- AvailabilityUtils.rectangle(dragStartDay, dragStartTime, dragEndDay, dragEndTime)) {
      $[html.TableCell](table, s"tr[data-day='$day'] td[data-time='$time']").classList.add(dragMode)
    }
  }

  private def tableCellHandler(handler: (html.TableCell, LocalDate, LocalTime) => Unit): MouseEvent => Unit =
    (ev: MouseEvent) => {
      ev.target match {
        case target: html.Element =>
          target.closest("td") match {
            case cell: html.TableCell =>
              val day  = cell.closest("[data-day]")
              val time = cell.closest("[data-time]")
              if (day != null && time != null) {
                handler(cell, LocalDate.parse(day.dataset.day.get), LocalTime.parse(time.dataset.time.get))
              }
            case _ => // Ignore
          }
        case _ => // Ignore
      }
    }

  private val btn = $[html.Button]("#availability-index button")

  btn.onclick = { ev =>
    val data = Json.toJson(generateRanges())
    Http.post(Router.Availability.save.url, data)
  }

  private def generateRanges(): Seq[(String, String)] = {
    $$[html.TableRow](table, "tr")
      .filter(row => row.querySelector(s"td.$ClassAvailable") != null)
      .flatMap { row =>
        val day = LocalDate.parse(row.dataset.day.get)
        for ((start, end) <- readRow($$[html.TableCell](row, "td")))
          yield
            (AvailabilityUtils.pack(day, LocalTime.parse(start)).toString,
             AvailabilityUtils.pack(day, LocalTime.parse(end)).toString)
      }
  }

  private def readRow(cells: Seq[html.TableCell]): List[(String, String)] = {
    val (span, rest) = cells.dropWhile(isNotAvailable).span(isAvailable)
    if (span.isEmpty) Nil else (span.head.dataset.time.get, span.last.dataset.time.get) :: readRow(rest)
  }

  private def isAvailable(cell: html.TableCell): Boolean    = cell.classList.contains(ClassAvailable)
  private def isNotAvailable(cell: html.TableCell): Boolean = !isAvailable(cell)
}

object Availability {}
