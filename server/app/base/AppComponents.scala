package base

import akka.stream.Materializer
import db.api.Database
import javax.inject.{Inject, Provider}
import play.api.db.slick.DatabaseConfigProvider
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import scala.concurrent.ExecutionContext
import slick.jdbc.MySQLProfile

/**
  * Applications components.
  *
  * This class should be instantiated by Guice and injected in controllers and
  * services instances. This is a single dependency taking care of bringing in
  * everything shared by every controllers and services.
  *
  * This instance also emulates a [[ControllerComponents]] from Play and can be
  * used as a parameter for the [[AbstractController]] constructor.
  */
class AppComponents @Inject()(userActionProvider: Provider[UserAction],
                              checkAclProvider: Provider[CheckAcl],
                              dcp: DatabaseConfigProvider,
                              cc: ControllerComponents)
                             (val materializer: Materializer)
	extends ControllerComponents {

	// Provider accessors
	def userAction: UserAction = userActionProvider.get()
	def checkAcl: CheckAcl = checkAclProvider.get()

	// Database from configuration
	val database: Database = dcp.get[MySQLProfile].db

	// Masquerade a standard Play ControllerComponents
	val actionBuilder: ActionBuilder[Request, AnyContent] = cc.actionBuilder
	val parsers: PlayBodyParsers = cc.parsers
	val messagesApi: MessagesApi = cc.messagesApi
	val langs: Langs = cc.langs
	val fileMimeTypes: FileMimeTypes = cc.fileMimeTypes
	val executionContext: ExecutionContext = cc.executionContext
}

object AppComponents {
	/**
	  * Exposes usual implicits from AppComponents.
	  */
	trait Implicits {
		private[base] val cc: AppComponents

		protected final implicit def executionContext: ExecutionContext = cc.executionContext
		protected final implicit def materializer: Materializer = cc.materializer
		protected final implicit def database: Database = cc.database
	}
}
