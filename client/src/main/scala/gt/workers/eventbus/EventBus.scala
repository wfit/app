package gt.workers.eventbus

import gt.GuildTools
import gt.util.Http
import gt.workers.{AutoWorker, Worker, WorkerRef}
import models.eventbus.Event
import org.scalajs.dom
import platform.JsPlatform._
import play.api.libs.json.Json
import protocol.CompoundMessage._
import scala.concurrent.duration._
import scala.scalajs.js
import utils.UUID

class EventBus extends Worker {
	private var uuid = UUID.zero
	private var bindings = Set.empty[(String, WorkerRef)]
	private var closed = false
	private var bus: dom.EventSource = null

	private def open(): Unit = {
		bus = new dom.EventSource("/events/bus")

		bus.onmessage = { msg =>
			Event.fromJson(msg.data.asInstanceOf[String]) match {
				case e @ Event("eventbus.instanceid") => // Ignore
					val id = e.data.asInstanceOf[UUID]
					if (GuildTools.instanceUUID != id.toString) {
						GuildTools.reload()
					}
				case e @ Event("eventbus.streamid") =>
					uuid = e.data.asInstanceOf[UUID]
					val subscriptions = bindings.map { case (c, _) => c }
					if (subscriptions.nonEmpty) {
						Http.post("/events/subscribe-all", Json.obj(
							"stream" -> uuid,
							"channels" -> subscriptions.toSeq
						))
					}
				case Event("eventbus.keepalive") => // Ignore
				case e: Event if e.system =>
					dom.console.warn("Ignoring system event:", e.channel, e.payload)
				case e @ Event(channel) =>
					val event = e.compound
					bindings
						.collect { case (c, w) if c == channel => w }
						.foreach(_ ! event)
			}
		}

		bus.onerror = { _: js.Any =>
			if (!closed) {
				bus.close()
				js.timers.setTimeout(5.seconds) {
					if (!closed) open()
				}
			}
		}
	}

	open()

	def receive: Receive = {
		case 'Subscribe ~ (channel: String) if sender != WorkerRef.NoWorker =>
			if (!bindings.exists { case (c, _) => c == channel }) {
				Http.post("/events/subscribe", Json.obj("stream" -> uuid, "channel" -> channel))
			}
			bindings += (channel -> sender)

		case 'Unsubscribe ~ (channel: String) =>
			val oldSize = bindings.size
			val pair = (channel, sender)
			bindings = bindings.filterNot(_ == pair)
			if (oldSize != bindings.size) {
				Http.post("/events/unsubscribe", Json.obj("stream" -> uuid, "channel" -> channel))
			}
	}

	override def onTerminate(): Unit = {
		closed = true
		bus.close()
	}
}

object EventBus extends AutoWorker.Named[EventBus]("eventbus") {
	def subscribe(channel: String)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = ref ! 'Subscribe ~ channel
	def unsubscribe(channel: String)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = ref ! 'Unsubscribe ~ channel
}
