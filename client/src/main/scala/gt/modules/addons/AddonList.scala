package gt.modules.addons

import gt.util.View
import gt.workers.Worker
import gt.workers.updater.{Manifest, Updater}
import gt.{GuildTools, Settings}
import mhtml._
import org.scalajs.dom
import org.scalajs.dom.html
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

class AddonList extends Worker with View {
	private val updater = Updater.ref
	updater ! 'RegisterView ~ self

	private val status = Var("loading")
	private val color = Var("#999")
	private val text = Var("Chargement")
	private val path = Var(Option(Settings.UpdaterPath.get))
	private val message = Var(None: Option[String])
	private val manifest = Var(Manifest.empty)

	mount("#updater-status-container") {
		val msg = message.map(_.map { m =>
			<tr>
				<td colspan="2" class="gray small">
					{m}
				</td>
			</tr>
		})
		val actions = status.map {
			case "enabled" =>
				<div class="row">
					<button class="flex alternate" onclick={() => updater ! 'Update}>Actualiser</button>
					<button class="flex alternate" onclick={() => disableUpdater()}>Désactiver</button>
				</div>
			case "failure" =>
				<div class="row">
					<button class="flex alternate" onclick={() => respawnUpdater()}>Réessayer</button>
					<button class="flex alternate" onclick={() => disableUpdater()}>Désactiver</button>
				</div>
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
		<table class="box no-hover">
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
			Settings.UpdaterPath := choice
			path := Some(choice)
			manifest := Manifest.empty
			updater.respawn()
		}
	}

	private def respawnUpdater(): Unit = {
		updater.respawn()
	}

	private def disableUpdater(): Unit = {
		Settings.UpdaterPath := null
		path := None
		updater.respawn()
	}

	mount("#addons-list-container") {
		val togglesDisabled = status.map(_ != "enabled")
		def state(addon: Manifest.Addon): String = {
			(addon.installed, addon.managed, addon.sync) match {
				case (false, _, _) => "Absent"
				case (true, false, _) => "Ignoré"
				case (true, true, false) => "Synchronisation..."
				case (true, true, true) => "À jour"
			}
		}
		def formatDate(date: String): String = {
			date.replaceFirst("""\d{4}-(\d{2})-(\d{2})T(\d{2}):(\d{2}).*""", "$2/$1 – $3:$4")
		}
		val rows = manifest.map(_.addons.map { addon =>
			<tr>
				<td><input type="checkbox" checked={addon.managed} disabled={togglesDisabled} onclick={toggleAddonState(addon.name)(_)} /></td>
				<td>{addon.name}</td>
				<td>{state(addon)}</td>
				<td>{addon.rev.substring(0, 8)}</td>
				<td>{formatDate(addon.date)}</td>
			</tr>
		})
		<table class="box full no-hover fixed">
			<tr>
				<th style="width: 35px;"></th>
				<th>Addon</th>
				<th style="width: 200px;">État</th>
				<th style="width: 150px;">Révision</th>
				<th style="width: 150px;">Date</th>
			</tr>
			<tbody>{rows}</tbody>
		</table>
	}

	private def toggleAddonState(addon: String)(event: dom.Event): Unit = {
		event.preventDefault()
		val action = if (event.target.asInstanceOf[html.Input].checked) 'Install else 'Uninstall
		updater ! action ~ addon
	}

	def receive: Receive = {
		case 'SetStatus ~ (status: String) ~ (color: String) ~ (text: String) =>
			this.status := status
			this.color := color
			this.text := text

		case 'SetMessage ~ msg =>
			this.message := Option(msg.asInstanceOf[String])

		case 'SetManifest ~ (manifest: Manifest) =>
			this.manifest := manifest
	}

	override def onTerminate(): Unit = {
		updater ! 'UnregisterView
	}
}
