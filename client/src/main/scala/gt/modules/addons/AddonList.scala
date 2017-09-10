package gt.modules.addons

import gt.GuildTools
import gt.tools.{ViewUtils, WorkerView}
import gt.workers.Worker
import gt.workers.updater.Updater
import mhtml._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.Dynamic.literal

class AddonList extends Worker with ViewUtils {
	private val updater = Updater.ref
	updater ! 'RegisterView ~ self

	private val status = Var("loading")
	private val color = Var("#999")
	private val text = Var("Chargement")
	private val path = Var(Option(dom.window.localStorage.getItem("updater.path")))
	private val message = Var(None: Option[String])

	mount("#updater-status-container") {
		val msg = message.map(_.map { m =>
			<tr>
				<td colspan="2" class="gray">
					{m}
				</td>
			</tr>
		})
		val actions = status.map {
			case "disabled" =>
				<div class="row">
					<button class="flex alternate" onclick={() => selectWowDirectory()}>Activer</button>
				</div>
			case _ => null
		}.map(content => Option(content).map { content =>
			<tr>
				<td colspan="2" class="no-padding">
					{content}
				</td>
			</tr>
		})
		<table class="box inline no-hover" style="width: 500px;">
			<tr>
				<th style="width: 75px;">Statut</th>
				<td>
					<span class="dot" style={color.map(c => s"background-color: $c")}></span>
					<span>
						{text}
					</span>
				</td>
			</tr>
			<tr>
				<th>Dossier</th>
				<td>{path map (_ getOrElse "—")}</td>
			</tr>{msg}{actions}
		</table>
	}

	private def selectWowDirectory(): Unit = {
		GuildTools.remote.dialog.showOpenDialog(
			GuildTools.remote.getCurrentWindow(),
			literal(
				title = "Sélectionnez le dossier d'installation de World of Warcraft",
				defaultPath = "C:\\Program Files (x86)\\World of Warcraft",
				properties = js.Array("openDirectory")
			)
		).foreach { res =>
			val choice = res.head
			dom.window.localStorage.setItem("updater.path", choice)
			path := Some(choice)
			updater.respawn()
		}
	}

	def receive: Receive = {
		case 'SetStatus ~ (status: String) ~ (color: String) ~ (text: String) =>
			this.status := status
			this.color := color
			this.text := text

		case 'SetMessage ~ msg =>
			this.message := Option(msg.asInstanceOf[String])
	}

	override def onTerminate(): Unit = {
		updater ! 'UnregisterView
	}
}

@JSExportTopLevel("views.addons.list")
object AddonList extends WorkerView[AddonList]
