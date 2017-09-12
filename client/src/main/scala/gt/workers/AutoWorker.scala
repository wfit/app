package gt.workers

import gt.GuildTools
import org.scalajs.dom
import scala.reflect.ClassTag
import scala.scalajs.reflect.Reflect
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
abstract class AutoWorker [W <: Worker : ClassTag] {
	private[AutoWorker] var _ref: WorkerRef = WorkerRef.NoWorker
	final def ref: WorkerRef = _ref

	def spawn(): WorkerRef = Worker.local[W]

	def start(): Unit = {
		if (_ref ne WorkerRef.NoWorker) throw new IllegalStateException("Worker already started")
		_ref = spawn()
	}
}

object AutoWorker {
	def start(workers: Seq[String]): Unit = {
		for (fqcn <- workers) {
			Reflect.lookupLoadableModuleClass(fqcn) orElse Reflect.lookupLoadableModuleClass(fqcn + "$") match {
				case Some(module) => module.loadModule().asInstanceOf[AutoWorker[_]].start()
				case None => dom.console.error(s"Unable to start auto-worker: $fqcn (class not found)")
			}
		}
	}

	abstract class Named [W <: Worker : ClassTag] (val name: String) extends AutoWorker[W] {
		_ref = GuildTools.sharedWorkers.get(name).map(WorkerRef.fromString) getOrElse WorkerRef.NoWorker

		override def spawn(): WorkerRef = {
			val ref = super.spawn()
			GuildTools.sharedWorkers.put(name, ref.toString)
			ref
		}
	}

	abstract class Spawn [W <: Worker : ClassTag] extends AutoWorker[W] {
		override def spawn(): WorkerRef = Worker.spawn[W]
	}
}
