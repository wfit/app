package facades.node

import scala.concurrent.Future
import scala.scalajs.js

@js.native
trait FileSystem extends js.Object {
	def readdir(path: String, cb: Callback[js.Array[String]]): Unit = js.native
	def readdirSync(path: String): js.Array[String] = js.native
	def stat(path: String, cb: Callback[Stats]): Unit = js.native
	def lstat(path: String, cb: Callback[Stats]): Unit = js.native
	def statSync(path: String): Stats = js.native
	def createReadStream(path: String): Stream.Read = js.native
	def createWriteStream(path: String): Stream.Write = js.native
	def readFile(path: String, encoding: String, cb: Callback[String]): Unit = js.native
	def writeFile(path: String, data: String, encoding: String, cb: Callback[Unit]): Unit = js.native
	def mkdir(path: String, cb: Callback[Unit]): Unit = js.native
	def rmdir(path: String, cb: Callback[Unit]): Unit = js.native
	def unlink(path: String, cb: Callback[Unit]): Unit = js.native
	def rename(oldPath: String, newPath: String, cb: Callback[Unit]): Unit = js.native
}

object FileSystem extends NodeModule[FileSystem]("fs") {
	implicit class FileSystemOps (private val fs: FileSystem) extends AnyVal {
		def stat(path: String): Future[Stats] = async[Stats](fs.stat(path, _))
		def lstat(path: String): Future[Stats] = async[Stats](fs.lstat(path, _))
		def readFile(path: String, encoding: String): Future[String] = async[String](fs.readFile(path, encoding, _))
		def writeFile(path: String, data: String, encoding: String): Future[Unit] = async[Unit](fs.writeFile(path, data: String, encoding, _))
		def readdir(path: String): Future[js.Array[String]] = async[js.Array[String]](fs.readdir(path, _))
		def mkdir(path: String): Future[Unit] = async[Unit](fs.mkdir(path, _))
		def rmdir(path: String): Future[Unit] = async[Unit](fs.rmdir(path, _))
		def unlink(path: String): Future[Unit] = async[Unit](fs.unlink(path, _))
		def rename(oldPath: String, newPath: String): Future[Unit] = async[Unit](fs.rename(oldPath, newPath, _))
	}
}
