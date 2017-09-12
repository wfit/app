package gt.workers

import gt.workers.AskWorker.DoAsk
import protocol.MessageSerializer
import scala.concurrent.Promise

class AskWorker extends Worker {
	private var promise: Promise[Any] = _

	def receive: Receive = {
		case a @ DoAsk(dest, msg, p) =>
			implicit val s: MessageSerializer[Any] = a.serializer
			promise = p
			dest ! msg
			become(waiting)
		case other =>
			throw new IllegalArgumentException("Expecting a DoAsk message, got: " + other)
	}

	def waiting: Receive = {
		case response =>
			promise.success(response)
			terminate()
	}
}

object AskWorker {
	case class DoAsk[T](dest: WorkerRef, msg: T, promise: Promise[Any])(implicit val serializer: MessageSerializer[T])
}
