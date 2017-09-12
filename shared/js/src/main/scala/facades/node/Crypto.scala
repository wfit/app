package facades.node

import scala.concurrent.Future
import scala.scalajs.js

@js.native
trait Crypto extends js.Object {
	def createHash(algo: String): Hash = js.native
}

object Crypto {
	implicit class CryptoOps (private val crypto: Crypto) extends AnyVal {
		def sha1Stream(file: Stream.Read): Future[String] = {
			val hash = crypto.createHash("sha1").setEncoding("hex")
			file.pipe(hash)
			async[String] { cb =>
				hash.on("finish", () => cb(null, hash.read()))
				file.on("error", (e: js.Any) => {
					hash.close()
					cb(e, null)
				})
			}
		}
	}
}
