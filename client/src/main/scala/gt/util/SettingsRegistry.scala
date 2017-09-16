package gt.util

import gt.GuildTools
import gt.workers.ui.UIWorker
import org.scalajs.dom
import protocol.CompoundMessage._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SettingsRegistry {
	var settings = Map.empty[String, SettingApi[_]]

	protected class Typing[U](val to: String => U)(val from: U => String)
	protected object BooleanT extends Typing(_ != "0")(if (_) "1" else "0")

	trait SettingApi[T] { self =>
		def := (newValue: T): Unit
		def value: Future[T]
		def get: T

		def key: String
		def default: T

		settings += key -> this

		def transform[U](tu: T => U, ut: U => T): SettingApi[U] = new SettingApi[U] {
			def := (newValue: U): Unit = self := ut(newValue)
			def value: Future[U] = self.value.map(tu)
			def get: U = tu(self.get)

			def key: String = self.key
			def default: U = tu(self.default)
		}

		def typed[U](tr: Typing[U])(implicit ev: T =:= String): SettingApi[U] = {
			transform[U]((t: T) => tr.to(ev(t)), (u: U) => tr.from(u).asInstanceOf[T])
		}

		def bind(action: T => Unit): SettingApi[T] = {
			if (GuildTools.isWorker) this
			else new SettingApi[T] {
				def := (newValue: T): Unit = { self := newValue; action(newValue) }
				def value: Future[T] = self.value
				def get: T = self.get
				def key: String = self.key
				def default: T = self.default
			}
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
}
