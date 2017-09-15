package services

import akka.NotUsed
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import javax.inject.{Inject, Singleton}
import models.eventbus.Event
import protocol.MessageSerializer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import services.EventBus.Stream
import utils.UUID

@Singleton
class EventBus @Inject()(val runtimeService: RuntimeService)
                        (implicit private val materializer: Materializer,
                         private val executionContext: ExecutionContext) {
	private val (queue, out) =
		Source.queue[Event](bufferSize = 64, OverflowStrategy.dropHead)
			.toMat(BroadcastHub.sink[Event])(Keep.both)
			.mapMaterializedValue { case (q, o) => o.runWith(Sink.ignore); (q, o) }
			.run()

	private var streams = Map.empty[UUID, Stream]
	private var users = Map.empty[UUID, Set[Stream]]

	def openStream(owner: UUID): Stream = {
		Stream(UUID.random, owner)(this, out)
	}

	private def open(stream: Stream): Unit = synchronized {
		streams += (stream.id -> stream)
		users += (stream.owner -> (users.getOrElse(stream.owner, Set.empty) + stream))
	}

	private def closed(stream: Stream): Unit = synchronized {
		streams -= stream.id
		for (set <- users.get(stream.owner).map(_ - stream)) {
			if (set.isEmpty) users -= stream.owner
			else users += stream.owner -> set
		}
	}

	def publish[D: MessageSerializer](channel: String, data: D): Unit = {
		val event = Event(channel, data)
		queue.offer(event)
	}

	def getStream(uuid: UUID, owner: UUID): Option[Stream] = {
		streams.get(uuid).filter(_.owner == owner)
	}
}

object EventBus {
	private val Tick = Event("eventbus.keepalive", (), system = true).toString

	case class Stream private (id: UUID, owner: UUID)(bus: EventBus, busIn: Source[Event, NotUsed]) {
		private var filters = Set.empty[String]
		private def isSubscribed(channel: String): Boolean = filters contains channel

		def subscribe(channel: String): Unit = filters += channel
		def unsubscribe(channel: String): Unit = filters -= channel

		private val filteredBus =
			busIn.buffer(size = 16, OverflowStrategy.dropHead)
				.filter(event => event.system || isSubscribed(event.channel))
				.map(event => event.toString)

		private val prefixMessages = Source(List(
			Event("eventbus.instanceid", bus.runtimeService.instanceUUID, system = true),
			Event("eventbus.streamid", id, system = true)
		).map(_.toString))

		private val ((queue, done), pub) =
			Source.queue[String](bufferSize = 16, OverflowStrategy.dropHead)
				.merge(filteredBus)
				.prepend(prefixMessages)
				.keepAlive(30.seconds, () => Tick)
				.backpressureTimeout(30.seconds)
				.watchTermination()(Keep.both)
				.toMat(Sink.asPublisher(false))(Keep.both)
				.run()(bus.materializer)

		bus.open(this)
		done.onComplete(_ => bus.closed(this))(bus.executionContext)

		def out: Source[String, NotUsed] = Source.fromPublisher(pub)

		private def publishSerialized(channel: String, event: String): Unit = {
			if (isSubscribed(channel)) queue.offer(event)
		}

		def publish[D: MessageSerializer](channel: String, data: D): Unit = {
			publishSerialized(channel, Event(channel, data).toString)
		}
	}
}
