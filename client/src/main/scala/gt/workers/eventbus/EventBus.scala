package gt.workers.eventbus

import gt.GuildTools
import gt.util.Http
import gt.workers._
import gt.workers.eventbus.EventBus.ChannelSet
import models.eventbus.Event
import org.scalajs.dom
import platform.JsPlatform._
import play.api.libs.json._
import protocol.MessageSerializer
import scala.concurrent.duration._
import scala.scalajs.js
import utils.UUID

class EventBus extends Worker {
	private var uuid = UUID.zero
	private var bindings = Set.empty[(String, WorkerRef)]
	private var watched = Set.empty[WorkerRef]
	private var ready = false
	private var closed = false
	private var bus: dom.EventSource = null

	private def open(): Unit = {
		bus = new dom.EventSource("/events/bus")

		bus.onopen = { _: js.Any =>
			ready = true
		}

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
			ready = false
			if (!closed) {
				bus.close()
				js.timers.setTimeout(5.seconds) {
					if (!closed) open()
				}
			}
		}
	}

	open()

	private val KeepChannel: ((String, WorkerRef)) => String = { case (c, _) => c }
	private val KeepWorker: ((String, WorkerRef)) => WorkerRef = { case (_, w) => w }
	private def WithSender(sender: WorkerRef): String => (String, WorkerRef) = c => c -> sender

	def receive: Receive = {
		case 'Subscribe ~ ChannelSet(channels) if sender != WorkerRef.NoWorker =>
			val channelToSubscribe = channels diff bindings.map(KeepChannel)
			if (ready && channelToSubscribe.nonEmpty) {
				Http.post("/events/subscribe", Json.obj("stream" -> uuid, "channels" -> channelToSubscribe.toSeq))
			}
			if (!watched.contains(sender)) {
				watched += sender
				sender.watch()
			}
			bindings ++= channels.map(WithSender(sender))

		case 'Unsubscribe ~ ChannelSet(channels) =>
			val bindingsToRemove = channels.map(WithSender(sender))
			val bindingsToKeep = bindings diff bindingsToRemove
			val channelToUnsubscribe = bindingsToRemove.map(KeepChannel) diff bindingsToKeep.map(KeepChannel)
			if (ready && channelToUnsubscribe.nonEmpty) {
				Http.post("/events/unsubscribe", Json.obj("stream" -> uuid, "channels" -> channelToUnsubscribe.toSeq))
			}
			bindings = bindingsToKeep
			if (watched.contains(sender) && !bindings.map(KeepWorker).contains(sender)) {
				watched -= sender
				sender.unwatch()
			}

		case WorkerControl.Terminated =>
			val (collected, remaining) = bindings.partition { case (_, w) => w == sender }
			bindings = remaining
			watched -= sender
			val channelToUnsubscribe = collected.map(KeepChannel) diff remaining.map(KeepChannel)
			if (ready && channelToUnsubscribe.nonEmpty) {
				Http.post("/events/unsubscribe", Json.obj("stream" -> uuid, "channels" -> channelToUnsubscribe.toSeq))
			}
	}

	override def onTerminate(): Unit = {
		closed = true
		bus.close()
	}
}

object EventBus extends AutoWorker.Named[EventBus]("eventbus") {
	import protocol.CompoundMessage._

	case class ChannelSet(channels: Set[String])

	implicit object ChannelSetFormat extends Format[ChannelSet] {
		def reads(json: JsValue): JsResult[ChannelSet] = json.validate[Seq[String]].map(_.toSet).map(ChannelSet.apply)
		def writes(cs: ChannelSet): JsValue = implicitly[Writes[Seq[String]]].writes(cs.channels.toSeq)
	}

	implicit object ChannelSetSerializer extends MessageSerializer.Json[ChannelSet](ChannelSetFormat)

	def subscribe(channel: String*)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = ref ! 'Subscribe ~ ChannelSet(channel.toSet)
	def unsubscribe(channel: String*)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = ref ! 'Unsubscribe ~ ChannelSet(channel.toSet)
}
