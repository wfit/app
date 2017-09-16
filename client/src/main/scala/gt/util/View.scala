package gt.util

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
trait View extends ViewUtils {
	def init(): Unit = ()
	def unload(): Unit = ()
}