package gt.workers

import org.scalajs.dom
import protocol.MessageSerializer
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import utils.UUID

sealed class WorkerRef private[workers] (val uuid: UUID) {
	def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = {
		Worker.send(uuid, sender.uuid, msg)
	}

	def ?[T: MessageSerializer] (msg: T)(implicit sender: WorkerRef = WorkerRef.NoWorker): Future[Any] = {
		val promise = Promise[Any]()
		val worker = Worker.local[AskWorker]
		worker !< AskWorker.DoAsk(this, msg, promise)
		promise.future
	}

	def terminate()(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = this ! WorkerControl.Terminate
	def respawn()(implicit sender: WorkerRef = WorkerRef.NoWorker): Unit = this ! WorkerControl.Respawn

	override def toString: String = uuid.toString
	override def hashCode(): Int = uuid.hashCode()
	override def equals(obj: Any): Boolean = obj match {
		case ref: WorkerRef => ref.uuid == uuid
		case _ => false
	}
}

object WorkerRef {
	val NoWorker: WorkerRef = new WorkerRef(UUID.zero) {
		override def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef): Unit = {
			dom.console.warn("Ignoring message sent to UUID.zero: ", msg.asInstanceOf[js.Any])
		}
		override def toString: String = "<no-worker>"
		override def hashCode(): Int = System.identityHashCode(this)
		override def equals(obj: Any): Boolean = obj match {
			case ref: AnyRef => this eq ref
			case _ => false
		}
	}

	val Ignore: WorkerRef = new WorkerRef(UUID.dummy) {
		override def ![T: MessageSerializer] (msg: T)(implicit sender: WorkerRef): Unit = ()
		override def toString: String = "<ignore>"
		override def hashCode(): Int = System.identityHashCode(this)
		override def equals(obj: Any): Boolean = obj match {
			case ref: AnyRef => this eq ref
			case _ => false
		}
	}

	final class Local (id: UUID) extends WorkerRef(id) {
		require(id != UUID.zero, "Local WorkerRef targeting UUID.zero are forbidden")
		def !< (msg: Any)(implicit sender: WorkerRef): Unit = {
			Worker.sendLocal(uuid, sender.uuid, msg)
		}
	}

	def fromUUID(uuid: UUID): WorkerRef = uuid match {
		case UUID.zero => NoWorker
		case UUID.dummy => Ignore
		case _ => new WorkerRef(uuid)
	}

	def fromString(string: String): WorkerRef = string match {
		case "<no-worker>" => NoWorker
		case "<ignore>" => Ignore
		case other => fromUUID(UUID(other))
	}

	implicit object Serializer extends MessageSerializer.Lambda[WorkerRef](_.toString, fromString)
}
