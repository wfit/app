import com.google.inject.AbstractModule
import java.util.TimeZone
import services.Cron

class Module extends AbstractModule {
	override def configure(): Unit = {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
		bind(classOf[Cron]).asEagerSingleton()
	}
}
