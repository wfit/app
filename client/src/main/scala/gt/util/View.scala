package gt.util

import gt.workers.{Worker, WorkerControl, WorkerRef}
import mhtml.{Cancelable, Rx, _}
import org.scalajs.dom.html
import scala.xml.{Atom, Group, Node}

/**
  * Utilities for view workers
  */
trait View extends Worker with ViewUtils {
	/** MonadicHTML mount setting */
	private[this] val mountSettings = new ProdSettings {}

	/** Mount a node inside the element matching the given selector */
	protected def mount(selector: String)(children: Node*): Cancelable = {
		val node = if (children.size == 1) children.head else Group(children)
		setupAutoUnmount(mhtml.mount($[html.Element](selector), node, mountSettings))
	}

	/** Mount a reactive value inside the element matching the given selector */
	protected def mount(selector: String, obs: Rx[Node]): Cancelable = {
		setupAutoUnmount(mhtml.mount($[html.Element](selector), new Atom(obs), mountSettings))
	}

	/** Setup auto unmount when the owner worker terminates */
	private def setupAutoUnmount(cancelable: Cancelable): Cancelable = {
		View.unmountWatcher !< View.Watch(self, cancelable)
		cancelable
	}
}

object View {
	/** A simple view that does not use message feature of workers */
	abstract class Simple extends Worker.Dummy with View

	/** Message sent to the unmount watcher to start watching for a worker termination */
	private case class Watch(owner: WorkerRef, cancelable: Cancelable)

	/**
	  * The Unmount Watcher worker is responsible for watching worker having mounted
	  * DOM nodes and automatically canceling the binding once they terminates.
	  */
	private class UnmountWatcher extends Worker {
		/** The set of watched actor */
		private var watched = Map.empty[WorkerRef, List[Cancelable]]

		protected def receive: Receive = {
			case Watch(owner, cancelable) =>
				if (!(watched contains owner)) owner.watch()
				watched += owner -> (cancelable :: watched.getOrElse(owner, Nil))

			case WorkerControl.Terminated =>
				for (mounts <- watched.get(sender)) {
					watched -= sender
					mounts.foreach(_.cancel)
				}
		}
	}

	/** The singleton instance of the Unmount Watcher */
	private lazy val unmountWatcher = Worker.local[UnmountWatcher]
}
