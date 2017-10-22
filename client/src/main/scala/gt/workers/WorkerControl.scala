package gt.workers

import protocol.MessageSerializer

/** A worker control message */
case class WorkerControl(symbol: Symbol) extends AnyVal

object WorkerControl {
	val Spawned = WorkerControl('Spawned)
	val Terminated = WorkerControl('Terminated)
	val Terminate = WorkerControl('Terminate)
	val Respawn = WorkerControl('Reboot)
	val Watch = WorkerControl('Watch)
	val Unwatch = WorkerControl('Unwatch)
	val Detach = WorkerControl('Detach)

	implicit object Serializer extends MessageSerializer.Using[WorkerControl, Symbol](_.symbol, WorkerControl.apply) {
		override def optimistic(value: WorkerControl): Boolean = value == Terminated
	}
}
