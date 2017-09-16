package gt

import gt.workers.ui.UIWorker
import org.scalajs.dom
import protocol.CompoundMessage._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Settings {
	trait SettingApi[T] { self =>
		def := (newValue: T): Unit
		def value: Future[T]
		def get: T

		def format[U](tu: T => U, ut: U => T): SettingApi[U] = new SettingApi[U] {
			def := (newValue: U): Unit = self := ut(newValue)
			def value: Future[U] = self.value.map(tu)
			def get: U = tu(self.get)
		}
	}

	case class Setting(key: String, default: String) extends SettingApi[String] {
		def := (newValue: String): Unit = {
			if (GuildTools.isWorker) UIWorker.ref ! 'LocalStorageSet ~ key ~ newValue
			else if (newValue == null) dom.window.localStorage.removeItem(key)
			else dom.window.localStorage.setItem(key, newValue)
		}

		def value: Future[String] = {
			if (GuildTools.isWorker) (UIWorker.ref ? ('LocalStorageGet ~ key)).mapTo[String]
			else Future.successful(get)
		}

		def get: String = Option(dom.window.localStorage.getItem(key)) getOrElse default
	}

	val LoginIdentifier = Setting("login.identifier", "")
	val UpdaterPath = Setting("updater.path", null)
}
