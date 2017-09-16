package gt.workers.updater

import facades.node
import gt.util.Http
import org.scalajs.dom.experimental.HttpMethod
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js

object Pipe {
	private val fs = node.require(node.FileSystem)

	def urlToFile(url: String, file: String): Future[Unit] = {
		Http.fetch(url, HttpMethod.GET, mode = Http.StreamResponse).flatMap { stream =>
			val in = stream.getReader()
			val out = fs.createWriteStream(file)
			val promise = Promise[Unit]()

			out.on("error", (e: js.Any) => {
				in.cancel(e)
				promise.failure(node.BoxedErrorObject(e))
			})

			def nextChunk: Future[Unit] = {
				in.read().toFuture.flatMap(chunk => {
					if (chunk.done) {
						out.endWait()
					} else {
						val buffer = node.Buffer.from(chunk.value.buffer)
						out.write(buffer)
						nextChunk
					}
				})
			}

			promise.completeWith(nextChunk).future
		}
	}
}
