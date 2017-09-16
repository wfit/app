package gt.workers.updater

import facades.node
import gt.{GuildTools, Settings}
import gt.util.Http
import gt.workers.{AutoWorker, Stash, Worker, WorkerRef}
import gt.workers.eventbus.EventBus
import gt.workers.updater.Digest._
import org.scalajs.dom
import scala.annotation.tailrec
import scala.concurrent.{ExecutionException, Future}
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

class Updater extends Worker with Stash {
	private var ui: WorkerRef = WorkerRef.Ignore
	private var status: Status = Status.Initializing
	private var message: String = _
	private var manifest: Manifest = _

	private var serverManifest: Updater.ServerManifest = _
	private var shouldUpdateAgain = false
	private var updatedThisRound = Set.empty[String]

	if (GuildTools.acl.can("addons.access")) init()
	else updateState { status = Status.Locked }

	private def init(): Unit = {
		for (path <- Settings.UpdaterPath.value) {
			if (path == null) updateState { status = Status.Disabled; }
			else {
				updateState { message = "Vérification du dossier d'installation..." }
				Updater.findAddonsDirectory(path) match {
					case Some(addonsDir) =>
						updateState { status = Status.Enabled; }
						Updater.path = addonsDir
						EventBus.subscribe("updater.notify")
						become(enabled)
						self !< 'Update
					case None =>
						updateState { status = Status.Failure; message = "Le dossier d'installation n'est pas valide." }
				}
			}
		}
	}

	private var _last_manifest_update = (WorkerRef.NoWorker, Manifest.empty)
	private def updateState(thunk: => Unit = ()): Unit = {
		thunk
		ui ! 'SetStatus ~ status.code ~ status.color ~ status.text
		ui ! 'SetMessage ~ message

		val mu_pair = (ui, manifest)
		if (manifest != null && mu_pair != _last_manifest_update) {
			ui ! 'SetManifest ~ manifest
			_last_manifest_update = mu_pair
		}
	}

	def receive: Receive = {
		case 'RegisterView ~ (worker: WorkerRef) => updateState { ui = worker }
		case 'UnregisterView => ui = WorkerRef.Ignore
	}

	def enabled: Receive = receive orElse {
		case 'Update | "updater.notify" ~ _ =>
			updateState { status = Status.Updating; message = "Mise à jour de la liste d'addons..." }
			shouldUpdateAgain = false
			updatedThisRound = Set.empty
			become(updating)
			Http.get("/addons/manifest").flatMap {
				case res if res.ok =>
					serverManifest = res.as[Updater.ServerManifest]
					Updater.buildManifest(serverManifest)
				case res =>
					throw new Exception(res.status.toString)
			}.onComplete {
				case Success(mfst) =>
					updateState { manifest = mfst }
					self !< 'UpdateOne
				case Failure(e) =>
					updateState { message = s"Erreur lors de la récupération du manifest: ${ e.getMessage }" }
					become(enabled)
			}

		case 'Install ~ (name: String) =>
			manifest.addons.find(_.name == name) match {
				case None => dom.console.error(s"Unknown addon '$name'")
				case Some(addon) =>
					become(updating)
					updateState { status = Status.Updating; message = s"Installation de $name..." }
					Updater.installAddon(addon, this).onComplete(self !< ('Continue, _))
			}

		case 'Uninstall ~ (name: String) =>
			manifest.addons.find(_.name == name).filter(_.managed) match {
				case None => dom.console.error(s"Unknown addon '$name'")
				case Some(addon) =>
					become(updating)
					updateState { status = Status.Updating; message = s"Désinstallation de $name..." }
					Updater.uninstallAddon(addon, this).onComplete(self !< ('Continue, _))
			}
	}

	def updating: Receive = receive orElse {
		case 'UpdateOne =>
			manifest.addons.find(!_.sync) match {
				case None =>
					updatedThisRound = Set.empty
					self !< 'Done
				case Some(addon) if updatedThisRound contains addon.name =>
					// Ensure that we are not caught in an endless loop trying to update the same addon every time
					self !< ('Fail, s"La mise à jour de '${ addon.name }' a échoué.")
				case Some(addon) =>
					updateState { message = s"Mise à jour de '${ addon.name }'..." }
					updatedThisRound += addon.name
					Updater.syncAddon(addon, this).onComplete(self !< ('Continue, _))
			}

		case ('Continue, res: Try[_]) =>
			Future.fromTry(res)
				.flatMap(_ => Updater.buildManifest(serverManifest))
				.onComplete {
				case Success(updatedManifest) =>
					updateState { manifest = updatedManifest }
					self !< 'UpdateOne
				case Failure(e: ExecutionException) =>
					self !< ('Fail, e.getCause.getMessage)
				case Failure(e) =>
					self !< ('Fail, e.getMessage)
			}

		case 'Done =>
			updateState { status = Status.Enabled; message = null }
			become(enabled)
			unstash()
			if (shouldUpdateAgain) self !< 'Update

		case ('Fail, cause: String) =>
			updateState { status = Status.Failure; message = cause }
			become(enabled)
			unstash()

		case 'Update | "updater.notify" ~ _ =>
			shouldUpdateAgain = true

		case _ => stash()
	}

	override def onRespawn(): Unit = {
		if (ui ne WorkerRef.Ignore) self ! 'RegisterView ~ ui
	}
}

object Updater extends AutoWorker.Spawn[Updater] {
	import scala.concurrent.ExecutionContext.Implicits.global

	type ServerAddon = js.Dictionary[String]
	type ServerManifest = js.Array[ServerAddon]

	private val fs = GuildTools.require[node.FileSystem]("fs")
	private var path: String = _

	private def findAddonsDirectory(wowDir: String): Option[String] = {
		for {
			interface <- fs.readdirSync(wowDir).find(_.toLowerCase == "interface")
			addons <- fs.readdirSync(s"$wowDir/$interface").find(_.toLowerCase == "addons")
		} yield s"$wowDir/$interface/$addons"
	}

	private def buildManifest(serverManifest: ServerManifest): Future[Manifest] = {
		Future.sequence(serverManifest.map(scanAddonStatus).toSeq).map(Manifest.apply)
	}

	private implicit final class ServerAddonOps (private val sa: ServerAddon) extends AnyVal {
		def name: String = sa("name")
		def rev: String = sa("rev")
		def date: String = sa("date")
		def hash: String = sa("hash")
	}

	private def scanAddonStatus(addon: ServerAddon): Future[Manifest.Addon] = {
		val dir = s"$path/${ addon.name }"
		fs.stat(dir)
			.map(_.isDirectory())
			.recover { case _ => false }
			.flatMap {
				case false =>
					// Not installed
					Future.successful(Manifest.Addon(addon.name, addon.rev, addon.date, Metadata.empty))
				case true =>
					fs.readFile(s"$dir/.pkg.metadata", "utf8").map(Metadata.fromJson).map { metadata =>
						// Managed
						Manifest.Addon(addon.name, addon.rev, addon.date, metadata,
							installed = true, managed = true,
							sync = metadata.hash == addon.hash, latest = addon.hash)
					}.recover {
						// Unmanaged
						case _ => Manifest.Addon(addon.name, addon.rev, addon.date, Metadata.empty, installed = true)
					}
			}
	}

	private def syncAddon(addon: Manifest.Addon, updater: Updater): Future[Unit] = {
		for {
			newDigest <- fetchDigest(s"/addons/digest/${ addon.name }")
			oldDigest <- buildDigest(
				newDigest.topLevelDirectories ++
				addon.metadata.topLevelDirectories ++
				Legacy.directories.getOrElse(addon.name, Seq.empty)
			)
			actions = oldDigest diff newDigest
			total = actions.size
			cb = (count: Int) => updater.updateState { updater.message = s"Mise à jour de '${ addon.name }'... (${ 100 * count / total }%)" }
			_ <- if (actions.isEmpty) Future.unit else executeActions(pack(actions), cb)
			_ <- commitUpdate(addon, newDigest.topLevelDirectories)
		} yield ()
	}

	private def fetchDigest(url: String): Future[Digest] = {
		Http.get(url).collect { case res if res.ok => res.text }.map(Digest.fromSource)
	}

	private def buildDigest(dirs: Set[String]): Future[Digest] = {
		Future.sequence(dirs.toSeq.map { name =>
			digestDirectory(name).map(dir => (name, dir)).map(Some.apply).recover { case _ => None }
		}).map(children => Digest(Digest.Directory("", children.collect { case Some(entry) => entry }.toMap)))
	}

	private def digestDirectory(dir: String): Future[Digest.Node] = {
		val fullPath = s"$path$dir"
		fs.readdir(fullPath).flatMap { files =>
			Future.sequence(files.filter(_ != ".pkg.metadata").toSeq.map { file =>
				val entryName = s"$dir/$file"
				val entryPath = s"$path$entryName"
				fs.lstat(entryPath).flatMap {
					case stat if stat.isFile() => lib.sha1File(entryPath).map(hash => Digest.File(entryName, hash))
					case stat if stat.isDirectory() => digestDirectory(entryName)
				}.map(node => (entryName, Option(node))).recover { case _ => (entryName, None) }
			}).map(children => Digest.Directory(dir, children.collect { case (name, Some(node)) => (name, node) }.toMap))
		}
	}

	private def pack(ops: Seq[DiffOp]): List[Seq[DiffOp]] = {
		@tailrec def splitAtBreaks(ops: Seq[DiffOp], acc: Vector[Seq[DiffOp]] = Vector.empty): Vector[Seq[DiffOp]] = {
			val n = ops.indexWhere(_.break)
			if (n < 0) acc :+ ops else ops.splitAt(n + 1) match { case (a, b) => splitAtBreaks(b, acc :+ a) }
		}
		splitAtBreaks(ops).flatMap(pack => pack.grouped(16)).toList
	}

	private def executeActions(ops: List[Seq[DiffOp]], cb: Int => Unit = { _ => () }, count: Int = 0): Future[Unit] = {
		cb(count)
		val batch = Future.traverse(ops.head)(executeAction)
		if (ops.tail.nonEmpty) batch.flatMap(res => executeActions(ops.tail, cb, count + res.size))
		else batch.map(_ => ())
	}

	private def executeAction(op: DiffOp): Future[Unit] = {
		op match {
			case CreateFile(file, hash) => Pipe.urlToFile(s"/addons/blob/$hash", s"$path$file")
			case UpdateFile(file, hash) => Pipe.urlToFile(s"/addons/blob/$hash", s"$path$file")
			case DeleteFile(file, _) => fs.unlink(s"$path$file")
			case CreateDirectory(dir, _) => fs.mkdir(s"$path$dir")
			case DeleteDirectory(dir, _) => fs.rmdir(s"$path$dir")
		}
	}

	private def commitUpdate(addon: Manifest.Addon, tlds: Set[String]): Future[Unit] = {
		val addonDir = s"$path/${ addon.name }"
		ensureDirExists(addonDir).flatMap { _ =>
			val meta = addon.metadata.copy(
				hash = addon.latest,
				topLevelDirectories = tlds
			)
			fs.writeFile(s"$addonDir/.pkg.metadata", meta.toJson, "utf8")
		}
	}

	private def ensureDirExists(path: String): Future[Unit] = {
		fs.lstat(path)
			.map(stats => (true, stats.isDirectory()))
			.recover { case _ => (false, false) }
			.flatMap {
				case (false, false) =>
					fs.mkdir(path)
				case (true, false) =>
					fs.rename(path, s"$path.old").flatMap(_ => fs.mkdir(path))
				case _ =>
					Future.unit
			}
	}

	private def installAddon(addon: Manifest.Addon, updater: Updater): Future[Unit] = {
		for {
			_ <- ensureDirExists(s"$path/${addon.name}")
			_ <- fs.writeFile(s"$path/${ addon.name }/.pkg.metadata", "", "utf8")
		} yield ()
	}

	private def uninstallAddon(addon: Manifest.Addon, updater: Updater): Future[Unit] = {
		for {
			_ <- fs.unlink(s"$path/${ addon.name }/.pkg.metadata")
			digest <- Updater.buildDigest(addon.metadata.topLevelDirectories)
			actions = digest diff Digest.empty
			total = actions.size
			cb = (count: Int) => updater.updateState { updater.message = s"Désinstallation de '${ addon.name }'... (${ 100 * count / total }%)" }
			_ <- executeActions(pack(actions), cb)
		} yield ()
	}
}

