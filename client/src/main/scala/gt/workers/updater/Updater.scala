package gt.workers.updater

import gt.GuildTools
import gt.workers.{Worker, WorkerRef}
import gt.workers.ui.UIWorker
import gt.workers.updater.Updater._
import scala.scalajs.js.annotation.JSExportTopLevel

class Updater extends Worker {
	private var ui: WorkerRef = WorkerRef.Ignore
	private var status: Status = Initializing
	private var message: String = _

	for (path <- (UIWorker.ref ? ('LocalStorageGet ~ "updater.path")).mapTo[String]) {
		if (path == null) updateState { status = Disabled; message = "Le dossier d'installation n'est pas défini" }
		else {
			updateState { message = "Vérification du dossier d'installation" }
		}
	}

	def receive: Receive = {
		case 'RegisterView ~ (worker: WorkerRef) =>
			updateState { ui = worker }

		case 'UnregisterView =>
			ui = WorkerRef.Ignore
	}

	def updateState(thunk: => Unit = ()): Unit = {
		thunk
		ui ! 'SetStatus ~ status.code ~ status.color ~ status.text
		ui ! 'SetMessage ~ message
	}

	override def onRespawn(): Unit = {
		if (ui ne WorkerRef.Ignore) self ! 'RegisterView ~ ui
	}
}

@JSExportTopLevel("updater")
object Updater {
	val ref: WorkerRef = if (GuildTools.isApp && !GuildTools.isWorker) Worker.spawn[Updater] else WorkerRef.NoWorker

	sealed abstract class Status (val code: String, val color: String, val text: String)
	case object Initializing extends Status("initializing", "yellow", "Initialisation")
	case object Disabled extends Status("disabled", "#999", "Désactivé")
}

