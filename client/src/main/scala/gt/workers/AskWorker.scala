package gt.workers

import gt.workers.AskWorker.DoAsk
import java.util.concurrent.TimeoutException
import protocol.MessageSerializer
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

/**
  * An utility worker used to implement the Ask pattern.
  *
  * This worker wait for a [[DoAsk]] message with details about the ask
  * to perform and will wait for a response from the target actor.
  *
  * Once a response is received, the actor will resolve the promise with
  * the received value.
  */
class AskWorker extends Worker {
	private var promise: Promise[Any] = _

	def receive: Receive = {
		case a @ DoAsk(dest, msg, p, delay) =>
			implicit val s: MessageSerializer[Any] = a.serializer
			promise = p
			dest ! msg
			for (d <- delay) schedule(d) { self !< AskWorker.Timeout }
			become(waiting)

		case other =>
			throw new IllegalArgumentException("Expecting a DoAsk message, got: " + other)
			terminate()
	}

	private def waiting: Receive = {
		case AskWorker.Timeout =>
			promise.failure(new TimeoutException)

		case response =>
			promise.success(response)
			terminate()
	}
}

object AskWorker {
	/** Description of the ask operation to perform */
	case class DoAsk[T](dest: WorkerRef, msg: T, promise: Promise[Any], delay: Option[FiniteDuration] = None)
	                   (implicit val serializer: MessageSerializer[T])

	/** A dummy object used to indicate timeout of the ask operation */
	case object Timeout
}
