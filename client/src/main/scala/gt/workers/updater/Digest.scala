package gt.workers.updater

import facades.node
import gt.GuildTools
import gt.workers.updater.Digest.{DiffOp, Directory}
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.Try

case class Digest (tree: Directory) {
	val topLevelDirectories: Set[String] = tree.children.values.map { node =>
		require(node.isInstanceOf[Directory], "addons files must be contained in a directory")
		node.path
	}.toSet

	def diff(other: Digest): Seq[DiffOp] = tree diff other.tree
}

object Digest {
	// CONSTANTS
	val empty: Digest = Digest(Directory("", Map.empty))

	// IMPORT STUFF
	private lazy val crypto = GuildTools.require[node.Crypto]("crypto")

	private def parseSource(source: String): Option[js.Dictionary[Any]] = {
		Try(JSON.parse(source).asInstanceOf[js.Dictionary[Any]]).toOption
	}

	def fromSource(source: String): Digest = {
		parseSource(source).map(tree => Digest(typeDirectory("", tree))) getOrElse empty
	}

	private def typeNode(name: String, node: Any): Node = node match {
		case fileHash: String => File(name, fileHash)
		case directoryTree: js.Object => typeDirectory(name, directoryTree.asInstanceOf[js.Dictionary[Any]])
	}

	private def typeDirectory(dirName: String, dirNode: js.Dictionary[Any]): Directory = {
		Directory(dirName, dirNode.map { case (name, node) =>
			val fullName = s"$dirName/$name"
			(fullName, typeNode(fullName, node))
		}.toMap)
	}

	// DIGEST ADT
	sealed trait Node {
		val path: String
		val hash: String
	}

	case class File(path: String, hash: String) extends Node

	case class Directory(path: String, children: Map[String, Node]) extends Node {
		val orderedChildren: Seq[Node] = {
			children.toSeq.sortBy { case (k, _) => k }.map { case (_, v) => v }
		}

		val hash: String = {
			val hasher = crypto.createHash("sha1")
			for (child <- orderedChildren) hasher.update(child.hash)
			hasher.digest("hex")
		}

		def get(entry: String): Option[Node] = {
			val search = entry.toLowerCase
			children.collectFirst { case (key, value) if key.toLowerCase == search => value }
		}

		def diff(other: Directory): Seq[DiffOp] = {
			if (hash == other.hash) Seq.empty
			else {
				other.orderedChildren.flatMap { node =>
					(get(node.path), node) match {
						case (None, b: File) => Seq(CreateFile(b.path, b.hash))
						case (Some(a: File), b: File) if a.hash != b.hash => Seq(UpdateFile(b.path, b.hash))
						case (Some(a: Directory), b: File) => a.deleteOps(break = true) :+ CreateFile(b.path, b.hash)

						case (None, b: Directory) => b.createOps
						case (Some(a: File), b: Directory) => DeleteFile(a.path, break = true) +: b.createOps
						case (Some(a: Directory), b: Directory) if a.hash != b.hash => a diff b

						case _ => Seq.empty
					}
				} ++ orderedChildren.flatMap { node =>
					(node, other.get(node.path)) match {
						case (a: File, None) => Seq(DeleteFile(a.path))
						case (a: Directory, None) => a.deleteOps()
						case _ => Seq.empty
					}
				}
			}
		}

		def createOps: Seq[DiffOp] = {
			CreateDirectory(path, break = orderedChildren.nonEmpty) +: orderedChildren.flatMap {
				case file: File => Seq(CreateFile(file.path, file.hash))
				case dir: Directory => dir.createOps
			}
		}

		def deleteOps(break: Boolean = false): Seq[DiffOp] = {
			val deleteChildOps = if (orderedChildren.isEmpty) Seq.empty else {
				val init = orderedChildren.init.flatMap {
					case file: File => Seq(DeleteFile(file.path))
					case dir: Directory => dir.deleteOps()
				}
				val last = orderedChildren.last match {
					case file: File => Seq(DeleteFile(file.path, break = true))
					case dir: Directory => dir.deleteOps(break = true)
				}
				init ++ last
			}
			deleteChildOps :+ DeleteDirectory(path, break)
		}
	}

	// DIFFS
	sealed trait DiffOp {
		val break: Boolean
	}

	// We need both Create and Update for file to be able to compute whether the
	// user will need to reload to use the new version
	case class CreateFile(path: String, hash: String) extends DiffOp { val break = false }
	case class UpdateFile(path: String, hash: String) extends DiffOp { val break = false }
	case class DeleteFile(path: String, break: Boolean = false) extends DiffOp
	case class CreateDirectory(path: String, break: Boolean = false) extends DiffOp
	case class DeleteDirectory(path: String, break: Boolean = false) extends DiffOp
}
