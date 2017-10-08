package gt.workers

import gt.GuildTools
import gt.util.Microtask
import org.scalajs.dom
import org.scalajs.dom.webworkers.DedicatedWorkerGlobalScope.{self => worker}
import org.scalajs.dom.window.location
import platform.JsPlatform._
import play.api.libs.json._
import protocol.{CompoundMessage, Message, MessageSerializer}
import protocol.CompoundMessage.CompoundBuilder
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.Dynamic.{global, literal, newInstance}
import scala.scalajs.reflect.Reflect
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation
import scala.util.DynamicVariable
import utils.UUID

@EnableReflectiveInstantiation
abstract class Worker {
	// Receive
	type Receive = PartialFunction[Any, Unit]
	def receive: Receive

	// Alias compound utilities for worker instances
	protected final type ~[A, B] = CompoundMessage.~[A, B]
	protected final val ~ : CompoundMessage.~.type = protocol.CompoundMessage.~
	@inline protected final implicit def ImplCompoundBuilder[T: MessageSerializer](value: T): CompoundBuilder[T] = new CompoundBuilder(value)
	protected final implicit val executionContext: ExecutionContext = ExecutionContext.global

	// Worker settings
	require(Worker.newWorkerProperties.value != Worker.dummyWorkerInitializer,
		"Worker constructed directly without calling spawn[] or local[].")
	private val (fqcn, uuid: UUID, parent) = Worker.newWorkerProperties.value

	// References
	implicit protected val self: WorkerRef.Local = new WorkerRef.Local(uuid)

	private var attached = parent != WorkerRef.NoWorker
	if (attached) parent.watch()

	private var _sender: WorkerRef = WorkerRef.NoWorker
	protected def sender: WorkerRef = _sender
	private var _current: Any = _
	protected def current: Any = _current

	// Watchers
	private var watchers = Set.empty[WorkerRef]

	// Dispatch
	private val beforeDispatch: Receive = {
		case WorkerControl.Terminate => terminate()
		case WorkerControl.Respawn => respawn()
		case WorkerControl.Detach => detach()
		case WorkerControl.Watch => watchers += sender
		case WorkerControl.Unwatch => watchers -= sender
		case WorkerControl.Terminated if sender == parent && attached => terminate()
	}

	private val afterDispatch: Receive = {
		case unknown => dom.console.warn("Message ignored: ", unknown.asInstanceOf[js.Any])
	}

	private def buildBehavior(base: Receive): Receive = {
		beforeDispatch orElse base orElse afterDispatch
	}

	final def become(newBehavior: Receive): Unit = {
		behavior = buildBehavior(newBehavior)
	}

	// Scheduling
	private var tasks = Map.empty[UUID, () => Unit]
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

	// Control
	def onTerminate(): Unit = ()
	def onRespawn(): Unit = onTerminate()

	final def detach(): Unit = if (attached) {
		attached = false
		parent.unwatch()
	}

	final def terminate(): Unit = {
		onTerminate()
		cleanup()
		Worker.terminated(uuid)
	}

	final def respawn(): Unit = {
		onRespawn()
		// In case onRespawn called terminated
		if (Worker.children contains uuid) {
			cleanup()
			Worker.localWithUUID(fqcn, uuid, parent)
		}
	}

	private def cleanup(): Unit = {
		for (cancel <- tasks.values) cancel()
		for (watcher <- watchers) watcher ! WorkerControl.Terminated
	}

	private var behavior: Receive = buildBehavior(receive)

	private[workers] final def dispatch(sender: UUID, message: Any): Unit = {
		_sender = WorkerRef.fromUUID(sender)
		_current = message
		try behavior(message)
		catch {
			case t: Throwable =>
				dom.console.error("Error while dispatching message", t.asInstanceOf[js.Any], message.asInstanceOf[js.Any])
		}
		_sender = WorkerRef.NoWorker
		_current = null
	}
}

object Worker {
	/** A dummy worker that does not receive messages,
	  * use that when interested by life-cycles events */
	abstract class Dummy extends Worker {
		def receive: Receive = { case _ => ??? }
	}

	// CHILDREN

	private sealed trait ChildWorker
	private case class RemoteWorker (worker: dom.webworkers.Worker) extends ChildWorker
	private case class LocalWorker (worker: Worker) extends ChildWorker

	private var children = Map.empty[UUID, ChildWorker]

	// ADAPTER

	private lazy val sharedEnv = Map[String, String](
		"GT_APP" -> JSON.stringify(GuildTools.isApp),
		"GT_APP_VERSION" -> JSON.stringify(GuildTools.version),
		"GT_AUTHENTICATED" -> JSON.stringify(GuildTools.isAuthenticated),
		"CLIENT_SCRIPTS" -> JSON.stringify(GuildTools.clientScripts),
		"USER_ACL" -> JSON.stringify(global.USER_ACL)
	).map { case (key, value) => s"$key = $value;" }.mkString("\n")

	private def sharedWorkersBinding: String = JSON.stringify(GuildTools.sharedWorkers)

	private lazy val importPaths = {
		val protocol = location.protocol
		val host = location.host
		val paths = for (script <- GuildTools.clientScripts) yield s"$protocol//$host$script"
		paths.map(path => JsString(path).toString).mkString(",")
	}

	private val blobOptions = literal(`type` = "text/javascript").asInstanceOf[dom.BlobPropertyBag]

	private def workerBridge(fqcn: String, id: UUID, parent: WorkerRef): String = s"""
		|window = self;
		|$sharedEnv
		|SHARED_WORKERS = $sharedWorkersBinding;
		|importScripts($importPaths);
		|init_worker(${ JsString(fqcn) }, ${ JsString(id.toString) }, ${ JsString(parent.uuid.toString) });
		|""".stripMargin.trim

	// SPAWNING

	def spawn[T <: Worker : ClassTag](implicit parent: WorkerRef = WorkerRef.NoWorker): WorkerRef = {
		val fqcn = implicitly[ClassTag[T]].runtimeClass.getName
		val id = UUID.random
		val blob = new dom.Blob(js.Array(workerBridge(fqcn, id, parent)), blobOptions)
		val url = dom.URL.createObjectURL(blob)
		val instance = newInstance(global.Worker)(url).asInstanceOf[dom.webworkers.Worker]
		instance.onmessage = messageRouter(id, instance)
		registerWorker(id, RemoteWorker(instance))
		new WorkerRef(id)
	}

	def local[T <: Worker : ClassTag](implicit parent: WorkerRef = WorkerRef.NoWorker): WorkerRef.Local = {
		localWithUUID(implicitly[ClassTag[T]].runtimeClass.getName, UUID.random, parent)
	}

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

	private def registerWorker(uuid: UUID, worker: ChildWorker): Unit = {
		val respawn = children contains uuid
		children += (uuid -> worker)
		if (!respawn && GuildTools.isWorker) {
			send(UUID.zero, uuid, WorkerControl.Spawned)
		}
	}

	// SENDING

	private[workers] def send[T](dest: UUID, sender: UUID, msg: T)
	                            (implicit serializer: MessageSerializer[T]): Unit = Microtask.schedule {
		children.get(dest) match {
			case Some(LocalWorker(child)) if serializer.symmetric(msg) => child.dispatch(sender, msg)
			case child => relay(Message.build(dest, sender, msg), child)
		}
	}

	private[workers] def relay(msg: Message): Unit = relay(msg, null)

	private def relay(orig: Message, instance: Option[ChildWorker]): Unit = {
		val msg = orig.copy(ttl = orig.ttl - 1)
		require(msg.ttl > 0, "Message expired: " + msg.toString)
		(if (instance == null) children.get(msg.dest) else instance) match {
			case Some(RemoteWorker(child)) => child.postMessage(msg.toString)
			case Some(LocalWorker(child)) => child.dispatch(msg.sender, msg.payload)
			case None if GuildTools.isWorker => worker.postMessage(msg.toString)
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

	private[workers] def sendLocal(dest: UUID, sender: UUID, msg: Any): Unit = Microtask.schedule {
		children.get(dest) match {
			case Some(LocalWorker(worker)) => worker.dispatch(sender, msg)
			case _ => dom.console.error(s"Worker '$dest' is not a local worker.")
		}
	}

	// INITIALIZING

	@JSExportTopLevel("init_worker")
	def init(fqcn: String, id: String, parent: String): Unit = {
		localWithUUID(fqcn, UUID(id), WorkerRef.fromString(parent))
		worker.onmessage = messageRouter(UUID.zero)
	}

	type MessageRouter = js.Function1[dom.MessageEvent, Unit]
	private def messageRouter(id: UUID, instance: dom.webworkers.Worker = null): MessageRouter = { event =>
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

	private def terminated(uuid: UUID): Unit = if (children contains uuid) {
		children -= uuid
		if (GuildTools.isWorker) {
			send(UUID.zero, uuid, WorkerControl.Terminated)
			if (children.isEmpty) worker.close()
		}
	}

	private val dummyWorkerInitializer = (null: String, UUID.zero, WorkerRef.NoWorker)
	private val newWorkerProperties = new DynamicVariable[(String, UUID, WorkerRef)](dummyWorkerInitializer)
}
