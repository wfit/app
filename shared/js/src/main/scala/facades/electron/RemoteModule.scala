package facades.electron

import scala.scalajs.js

@js.native
trait RemoteModule extends js.Object {
	def getCurrentWindow(): BrowserWindow = js.native
	def require[T](module: String): T = js.native
	val dialog: DialogModule = js.native
	val app: AppModule = js.native
}
