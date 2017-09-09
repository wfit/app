package gt.workers.updater

import gt.GuildTools
import gt.workers.{Worker, WorkerRef}
import scala.scalajs.js.annotation.JSExportTopLevel

class Updater extends Worker {
	println("Hello from updater worker")
	self ! "foo"
	println("After sending")

	def receive: Receive = {
		case "foo" =>
			println("Received foo")
	}
}

@JSExportTopLevel("updater")
object Updater {
	val ref: WorkerRef = if (GuildTools.isApp && !GuildTools.isWorker) Worker.spawn[Updater] else WorkerRef.NoSender
}

