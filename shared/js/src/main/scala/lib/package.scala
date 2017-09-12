import facades.node
import scala.concurrent.Future
import scala.scalajs.js.Dynamic.global

package object lib {
	private[lib] val fs = global.require("fs").asInstanceOf[node.FileSystem]
	private[lib] val crypto = global.require("crypto").asInstanceOf[node.Crypto]

	def sha1File(path: String): Future[String] = crypto.sha1Stream(fs.createReadStream(path))
}
