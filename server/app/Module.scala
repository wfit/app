import com.google.inject.AbstractModule
import java.util.TimeZone

class Module extends AbstractModule {
	override def configure(): Unit = {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
		requestInjection(utils.SlickAPI)
	}
}
