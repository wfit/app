package gt.workers

import protocol.MessageSerializer

case class WorkerControl(symbol: Symbol) extends AnyVal

object WorkerControl {
	val Spawned = WorkerControl('Spawned)
	val Terminated = WorkerControl('Terminated)

	val Terminate = WorkerControl('Terminate)
	val Respawn = WorkerControl('Reboot)

	implicit object Serializer extends MessageSerializer.Lambda[WorkerControl](
		wc => wc.symbol.name,
		sym => WorkerControl(Symbol(sym))
	)
}
