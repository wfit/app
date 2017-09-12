package facades

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

package object node {
	type Callback[T] = js.Function2[js.Any, T, Unit]

	def async[T](partial: node.Callback[T] => Unit): Future[T] = {
		val promise = Promise[T]()
		val callback: node.Callback[T] = (err, res) => {
			if (js.isUndefined(err) || err == null) promise.success(res)
			else promise.failure(BoxedErrorObject(err))
		}
		partial(callback)
		promise.future
	}

	case class BoxedErrorObject(error: js.Any) extends Error(error.toString)

	abstract class NodeModule[T] (val name: String)

	def require[T](module: String): T = Global.require(module).asInstanceOf[T]
	def require[T](module: NodeModule[T]): T = require[T](module.name)
}
