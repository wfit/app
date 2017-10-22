package gt.workers

import gt.GuildTools
import gt.util.Microtask
import org.scalajs.dom
import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope.{self => selfThread}
import org.scalajs.dom.webworkers.{Worker => Thread}
import org.scalajs.dom.window.location
import platform.JsPlatform._
import play.api.libs.json._
import protocol.CompoundMessage.CompoundBuilder
import protocol.{CompoundMessage, Message, MessageSerializer}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global, literal, newInstance}
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.reflect.Reflect
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
import scala.util.DynamicVariable
import scala.util.control.NonFatal
import utils.UUID

/**
  * A JavaScript worker.
  *
  * A worker is an isolated unit of computation that is able to communicate with
  * other actors by sending and receiving messages.
  *
  * Workers also have a strict life-cycle defining starting up, respawning and
  * terminating. Life-cycle events can be used to define isolated components
  * of the application that can be started and stopped at will.
  */
@EnableReflectiveInstantiation
abstract class Worker {
	// Ensure that the worker was not instantiated directly
	if (Worker.newWorkerProperties.value == Worker.dummyWorkerInitializer) {
		throw new IllegalStateException("Worker constructed directly without calling spawn[] or local[].")
	}

	/** Type of the receive function */
	type Receive = PartialFunction[Any, Unit]

	/** The receive function, handles received message */
	protected def receive: Receive

	// Alias compound utilities for worker instances
	protected final type ~[A, B] = CompoundMessage.~[A, B]
	protected final val ~ : CompoundMessage.~.type = protocol.CompoundMessage.~
	@inline protected final implicit def ImplCompoundBuilder[T: MessageSerializer](value: T): CompoundBuilder[T] = new CompoundBuilder(value)

	// Make execution context available instead actors
	protected final implicit val executionContext: ExecutionContext = ExecutionContext.global

	// Worker settings
	private val (fqcn, uuid, parent) = Worker.newWorkerProperties.value

	/** Reference to the actor itself, implicitly available */
	implicit protected val self: WorkerRef.Local = new WorkerRef.Local(uuid)

	// Check if the worker should attach to a parent
	private var attached = parent != WorkerRef.NoWorker
	if (attached) parent.watch()

	/** The message sender, should only be used instead the receive function */
	protected def sender: WorkerRef = _sender
	private var _sender: WorkerRef = WorkerRef.NoWorker

	/** The current message, should only be used instead the receive function */
	protected def current: Any = _current
	private var _current: Any = _

	/** The set of actors watched by this actor */
	private var watchers = Set.empty[WorkerRef]

	/** The set of internal messages that should not be sent to the receive function */
	private val beforeDispatch: Receive = {
		case WorkerControl.Terminate => terminate()
		case WorkerControl.Respawn => respawn()
		case WorkerControl.Detach => detach()
		case WorkerControl.Watch => watchers += sender
		case WorkerControl.Unwatch => watchers -= sender
		case WorkerControl.Terminated if sender == parent && attached => terminate()
	}

	/** Fallback for unknown messages that were not handled by the receive function */
	private val afterDispatch: Receive = {
		case unknown => dom.console.warn("Message ignored: ", unknown.asInstanceOf[js.Any])
	}

	/** Builds the complete actor behavior from the base receive function */
	private def buildBehavior(base: Receive): Receive = {
		beforeDispatch orElse base orElse afterDispatch
	}

	/** Swaps the actor behavior to use the new receive function */
	final def become(newBehavior: Receive): Unit = {
		behavior = buildBehavior(newBehavior)
	}

	/** The set of scheduled tasks for this actor */
	private var tasks = Map.empty[UUID, () => Unit]

	/**
	  * Schedules a new task.
	  *
	  * Tasks are a managed mechanism allowing actors to perform deferred and repeated
	  * actions. Such tasks are automatically canceled when the actor is stopped.
	  *
	  * @param delay   the delay between executions
	  * @param repeat  whether the task should be repeated
	  * @param instant whether the first execution of the task should happen immediately,
	  *                this option has no effect if the task is not repeatable
	  * @param action  the action to execute
	  * @return a task UUID that can be used to cancel it
	  */
	protected def schedule(delay: FiniteDuration, repeat: Boolean = false, instant: Boolean = false)
	                      (action: => Unit): UUID = {
		val task = UUID.random
		if (repeat) {
			val tid = js.timers.setInterval(delay)(action)
			tasks += (task -> (() => js.timers.clearInterval(tid)))
			if (instant) action
		} else {
			val tid = js.timers.setTimeout(delay)(action)
			tasks += (task -> (() => js.timers.clearTimeout(tid)))
		}
		task
	}

	/** Cancel the given task */
	protected def cancel(task: UUID): Unit = {
		for (cancel <- tasks.get(task)) {
			cancel()
			tasks -= task
		}
	}

	/** Called when the worker terminated,
	  * should be overridden by the worker implementation */
	def onTerminate(): Unit = ()

	/**
	  * Called when the worker respawns. Should be overridden by
	  * the worker implementation.
	  *
	  * This function is called instead of [[onTerminate]] when the worker
	  * respawns.  The default behavior is to call [[onTerminate]], but the
	  * actual worker implementation can chose not to do so by overriding
	  * this method.
	  *
	  * If the overridden implementation of the method calls [[terminate]],
	  * the worker will not respawn.
	  */
	def onRespawn(): Unit = onTerminate()

	/** Detaches this worker from its parent,
	  * the worker will not terminate when its parent terminate. */
	final def detach(): Unit = if (attached) {
		attached = false
		parent.unwatch()
	}

	/** Terminates the worker */
	final def terminate(): Unit = {
		onTerminate()
		cleanup()
		Worker.terminated(uuid)
	}

	/** Respawn the worker */
	final def respawn(): Unit = {
		onRespawn()
		// This check will fail if onRespawn called terminate
		if (Worker.children contains uuid) {
			cleanup()
			Worker.localWithUUID(fqcn, uuid, parent)
		}
	}

	/** Performs worker cleanup */
	private def cleanup(): Unit = {
		for (cancel <- tasks.values) cancel()
		for (watcher <- watchers) watcher ! WorkerControl.Terminated
	}

	/** The current worker behavior */
	private var behavior: Receive = buildBehavior(receive)

	/** Dispatches a message sent to this worker */
	private[workers] final def dispatch(sender: UUID, message: Any): Unit = {
		_sender = WorkerRef.fromUUID(sender)
		_current = message
		try behavior(message)
		catch {
			case NonFatal(t) =>
				dom.console.error("Error while dispatching message", t.asInstanceOf[js.Any], message.asInstanceOf[js.Any])
		}
		_sender = WorkerRef.NoWorker
		_current = null
	}
}

object Worker {
	/** A dummy worker that does not receive messages,
	  * use this as base class when interested by life-cycles events */
	abstract class Dummy extends Worker {
		protected def receive: Receive = PartialFunction.empty
	}

	// CHILDREN

	/** A child worker of this thread */
	private sealed trait ChildWorker

	/** A worker living in a child-thread */
	private case class RemoteWorker (thread: Thread) extends ChildWorker

	/** A worker living in this thread  */
	private case class LocalWorker (worker: Worker) extends ChildWorker

	/** The set of child workers, either in the same thread or a child thread */
	private var children = Map.empty[UUID, ChildWorker]

	// ADAPTER

	/** The shared environment variables between threads */
	private lazy val sharedEnv: String = Map[String, String](
		"GT_APP" -> JSON.stringify(GuildTools.isApp),
		"GT_APP_VERSION" -> JSON.stringify(GuildTools.version),
		"GT_AUTHENTICATED" -> JSON.stringify(GuildTools.isAuthenticated),
		"CLIENT_SCRIPTS" -> JSON.stringify(GuildTools.clientScripts),
		"USER_ACL" -> JSON.stringify(global.USER_ACL)
	).map { case (key, value) => s"$key = $value;" }.mkString("\n")

	/** The set of shared worker references,
	  * these references are captured when spawning a child thread, as such, it is
	  * required for shared worker to be started between spawning a child thread. */
	private def sharedWorkersBinding: String = JSON.stringify(GuildTools.sharedWorkers)

	/** The set of scripts to import in a child thread */
	private lazy val importPaths = {
		val protocol = location.protocol
		val host = location.host
		val paths = for (script <- GuildTools.clientScripts) yield s"$protocol//$host$script"
		paths.map(path => JsString(path).toString).mkString(",")
	}

	/** The thread init code to inject in child threads */
	private def threadInit(fqcn: String, id: UUID, parent: WorkerRef): String = s"""
		|window = self;
		|$sharedEnv
		|SHARED_WORKERS = $sharedWorkersBinding;
		|importScripts($importPaths);
		|init_worker(${ JsString(fqcn) }, ${ JsString(id.toString) }, ${ JsString(parent.uuid.toString) });
		|""".stripMargin.trim

	/** Options used for constructing the child thread source code blob */
	private val blobOptions = literal(`type` = "text/javascript").asInstanceOf[dom.BlobPropertyBag]

	// SPAWNING

	/** Spawns a new worker of type T in a new thread */
	def spawn[T <: Worker : ClassTag](implicit parent: WorkerRef = WorkerRef.NoWorker): WorkerRef = {
		val fqcn = implicitly[ClassTag[T]].runtimeClass.getName
		val id = UUID.random
		val blob = new dom.Blob(js.Array(threadInit(fqcn, id, parent)), blobOptions)
		val url = dom.URL.createObjectURL(blob)
		val thread = newInstance(global.Worker)(url).asInstanceOf[Thread]
		thread.onmessage = messageRouter(id, thread)
		registerWorker(id, RemoteWorker(thread))
		new WorkerRef(id)
	}

	/** Spawns a new worker of type T in this thread */
	def local[T <: Worker : ClassTag](implicit parent: WorkerRef = WorkerRef.NoWorker): WorkerRef.Local = {
		localWithUUID(implicitly[ClassTag[T]].runtimeClass.getName, UUID.random, parent)
	}

	/** Constructs a new local worker with the given UUID */
	private def localWithUUID(fqcn: String, uuid: UUID, parent: WorkerRef): WorkerRef.Local = {
		Reflect.lookupInstantiatableClass(fqcn) match {
			case Some(instantiatableClass) =>
				val instance = newWorkerProperties.withValue((fqcn, uuid, parent)) {
					instantiatableClass.newInstance().asInstanceOf[Worker]
				}
				registerWorker(uuid, LocalWorker(instance))
				new WorkerRef.Local(uuid)
			case None =>
				throw new ClassNotFoundException(s"Unable to find class '$fqcn'")
		}
	}

	/** Register a worker instance in the set of child workers */
	private def registerWorker(uuid: UUID, worker: ChildWorker): Unit = {
		// Is this worker a respawn of an existing worker?
		val respawn = children contains uuid
		children += (uuid -> worker)
		if (!respawn && GuildTools.isWorker) {
			// Announce worker spawn to parent thread
			send(UUID.zero, uuid, WorkerControl.Spawned)
		}
	}

	// SENDING

	/** Sends a message to a worker.
	  * This function can target worker living either in the current thread
	  * or another one, automatically using the appropriate transmission mode. */
	private[workers] def send[T](dest: UUID, sender: UUID, msg: T)
	                            (implicit serializer: MessageSerializer[T]): Unit = Microtask.schedule {
		children.get(dest) match {
			case Some(LocalWorker(child)) if serializer.symmetric(msg) => child.dispatch(sender, msg)
			case child => relay(Message.build(dest, sender, msg), child)
		}
	}

	/** Relays a message to a worker living in another thread */
	private[workers] def relay(msg: Message): Unit = relay(msg, null)

	/** Relays a message to a worker living in another thread (internal) */
	private def relay(orig: Message, instance: Option[ChildWorker]): Unit = {
		val msg = orig.copy(ttl = orig.ttl - 1)
		require(msg.ttl > 0, "Message expired: " + msg.toString)
		(if (instance == null) children.get(msg.dest) else instance) match {
			case Some(RemoteWorker(child)) => child.postMessage(msg.toString)
			case Some(LocalWorker(child)) => child.dispatch(msg.sender, msg.payload)
			case None if GuildTools.isWorker => selfThread.postMessage(msg.toString)
			case None if msg.optimistic => // Ignore
			case None => msg.payload match {
				case WorkerControl.Watch =>
					// Synthesize terminated event
					implicit val fakeSender: WorkerRef = WorkerRef.fromUUID(msg.dest)
					WorkerRef.fromUUID(msg.sender) ! WorkerControl.Terminated
				case WorkerControl.Unwatch => // Ignore too
				case _ =>
					dom.console.warn(s"Unable to deliver message to worker '${ msg.dest }': ", msg.toString)
			}
		}
	}

	/** Sends a message to a worker living in this thread */
	private[workers] def sendLocal(dest: UUID, sender: UUID, msg: Any): Unit = Microtask.schedule {
		children.get(dest) match {
			case Some(LocalWorker(worker)) => worker.dispatch(sender, msg)
			case _ => dom.console.error(s"Worker '$dest' is not a local worker.")
		}
	}

	// INITIALIZING

	/** Worker initialization for sub-threads */
	@JSExportTopLevel("init_worker")
	def init(fqcn: String, id: String, parent: String): Unit = {
		localWithUUID(fqcn, UUID(id), WorkerRef.fromString(parent))
		selfThread.onmessage = messageRouter(UUID.zero)
	}

	/** The message router at the WebWorker-level */
	private def messageRouter(id: UUID, instance: Thread = null): js.Function1[dom.MessageEvent, Unit] = { event =>
		val msg = Message.parse(event.data.asInstanceOf[String])
		if (msg.dest == UUID.zero) {
			msg.payload match {
				case WorkerControl.Spawned if instance != null =>
					if (!children.contains(msg.sender)) {
						registerWorker(msg.sender, RemoteWorker(instance))
					}
				case WorkerControl.Terminated =>
					terminated(msg.sender)
				case unsupported =>
					dom.console.error("Unsupported control message: ", unsupported.asInstanceOf[js.Any])
			}
		} else {
			relay(msg)
		}
	}

	/** Called by workers on termination.
	  * If no more worker are running in this thread, the thread is also terminated */
	private def terminated(uuid: UUID): Unit = if (children contains uuid) {
		children -= uuid
		if (GuildTools.isWorker) {
			send(UUID.zero, uuid, WorkerControl.Terminated)
			if (children.isEmpty) selfThread.close()
		}
	}

	/** The dummy worker initialization used to detect that a worker was constructed directly */
	private val dummyWorkerInitializer: (String, UUID, WorkerRef) = (null, UUID.zero, WorkerRef.NoWorker)

	/** The initialization parameters for the worker currently being constructed */
	private val newWorkerProperties = new DynamicVariable(dummyWorkerInitializer)
}
