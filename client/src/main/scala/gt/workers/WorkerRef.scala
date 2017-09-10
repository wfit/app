package gt.workers

import gt.tools.Microtask
import org.scalajs
import org.scalajs.dom
import protocol.MessageSerializer
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import utils.UUID

sealed class WorkerRef private[workers] (val uuid: UUID) {
	def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = {
		Worker.send(uuid, sender.uuid, msg)
	}

	override def toString: String = s"WorkerRef($uuid)"

	def ?[T: MessageSerializer] (msg: T)(implicit sender: WorkerRef = WorkerRef.NoWorker): Future[Any] = {
		val promise = Promise[Any]()
		val worker = Worker.local[AskWorker]
		worker ! AskWorker.DoAsk(this, msg, promise)
		promise.future
	}

	def terminate()(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = this ! WorkerControl.Terminate
	def respawn()(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = this ! WorkerControl.Respawn
}

object WorkerRef {
	val NoWorker: WorkerRef = new WorkerRef(UUID.zero) {
		override def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef): Unit = {
			dom.console.warn("Ignoring message sent to UUID.zero: ", msg.asInstanceOf[js.Any])
		}
	}

	val Ignore: WorkerRef = new WorkerRef(UUID.zero) {
		override def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef): Unit = ()
	}

	final class Local (id: UUID) extends WorkerRef(id) {
		require(id != UUID.zero, "Local WorkerRef targeting UUID.zero are forbidden")
		def ! (msg: Any)(implicit sender: WorkerRef): Unit = Microtask.schedule {
			Worker.sendLocal(uuid, sender.uuid, msg)
		}
	}

	implicit object Serializer extends MessageSerializer[WorkerRef] {
		def serialize(ref: WorkerRef): String = ref.uuid.toString
		def deserialize(uuid: String): WorkerRef = new WorkerRef(UUID(uuid))
	}

	def fromUUID(uuid: UUID): WorkerRef = new WorkerRef(uuid)
}
