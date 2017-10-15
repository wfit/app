package controllers

import controllers.base.UserAction
import javax.inject._
import play.api.cache.Cached
import play.api.mvc._
import play.api.routing.{JavaScriptReverseRoute, JavaScriptReverseRouter}
import scala.concurrent.ExecutionContext
import services.AuthService
import utils.CustomStatus

@Singleton
class HomeController @Inject()(userAction: UserAction, cached: Cached)
                              (authService: AuthService)
                              (implicit executionContext: ExecutionContext)
	extends InjectedController {

	def index = userAction { req =>
		if (req.authenticated) Redirect(routes.DashboardController.dashboard())
		else Redirect(routes.HomeController.login())
	}

	def login(continue: Option[String]) = userAction.unauthenticated { implicit req =>
		Ok(views.html.home.login())
	}

	def performLogin = userAction.unauthenticated(parse.json).async { implicit req =>
		val id = (req.body \ "id").as[String]
		val pass = (req.body \ "pass").as[String]
		authService.login(id, pass).flatMap(authService.createSession).map { id =>
			Redirect(routes.DashboardController.dashboard(), CustomStatus.FullRedirect).withSession("key" -> id.toString)
		}
	}

	def logout = Action {
		Redirect(routes.HomeController.index()).withNewSession
	}

	def jsRouter(tag: String) = cached("router.js") {
		Action { implicit req =>
			import com.google.javascript.jscomp._
			def from(ctrl: Any): Seq[JavaScriptReverseRoute] = {
				ctrl.getClass.getDeclaredMethods.toSeq
					.filterNot(m => m.getName startsWith "_")
					.map(m => m.invoke(ctrl).asInstanceOf[JavaScriptReverseRoute])
			}
			val endpoints = Seq(
				from(routes.javascript.Assets),
				from(routes.javascript.AddonsController),
				from(routes.javascript.AdminController),
				from(routes.javascript.ComposerController),
				from(routes.javascript.DashboardController),
				from(routes.javascript.ElectronController),
				from(routes.javascript.EventBusController),
				from(routes.javascript.HomeController),
				from(routes.javascript.ProfileController),
				from(routes.javascript.RosterController),
				from(routes.javascript.SettingsController),
			).flatten
			val router = JavaScriptReverseRouter("routes")(endpoints: _*).body
			val compiler = new Compiler
			val options = new CompilerOptions
			CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
			compiler.compile(SourceFile.fromCode("extern.js", ""), SourceFile.fromCode("router.js", router), options)
			Ok(compiler.toSource).as("application/javascript")
		}
	}
}
