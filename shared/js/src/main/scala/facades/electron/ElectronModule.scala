package facades.electron

import scala.scalajs.js

@js.native
trait ElectronModule extends js.Object {
	val app: AppModule = js.native
	val dialog: DialogModule = js.native
	val Menu: MenuModule = js.native
	val remote: RemoteModule = js.native
	val webFrame: WebFrameModule = js.native
	val shell: ShellModule = js.native
}
