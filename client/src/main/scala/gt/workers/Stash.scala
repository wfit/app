package gt.workers

import gt.util.Microtask
import models.UUID

trait Stash extends Worker {
	private var buffer = List.empty[(UUID, Any)]

	def stash(): Unit = {
		buffer = (sender.uuid, current) :: buffer
	}

	def unstash(): Unit = {
		val stash = buffer
		buffer = Nil
		Microtask.schedule {
			stash.foreach { case (sender, message) => dispatch(sender, message) }
		}
	}
}
