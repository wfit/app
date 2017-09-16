package gt.util

import gt.workers.{Worker, WorkerRef}
import scala.reflect.ClassTag

abstract class WorkerView[W <: Worker : ClassTag] extends View {
	private var instance: WorkerRef.Local = _

	override def init(): Unit = {
		instance = Worker.local[W]
	}

	override def unload(): Unit = {
		instance.terminate()
		instance = null
	}
}
