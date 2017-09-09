package services

import controllers.routes
import javax.inject.Singleton
import scalajs.html.scripts
import utils.UUID

@Singleton
class RuntimeService {
	val instanceUUID: UUID = UUID.random

	private def selectFirstOf(scripts: String*): Option[String] = {
		scripts.find(name => getClass.getResource(s"/public/$name") != null)
			.map(name => routes.Assets.versioned(name).toString)
	}

	private def scriptForProject(project: String): Option[String] = {
		selectFirstOf(s"$project-opt.js", s"$project-fastopt.js")
	}

	private def dependenciesForProject(project: String): Option[String] = {
		selectFirstOf(s"$project-jsdeps.min.js", s"$project-jsdeps.js")
	}

	lazy val bootstrapScript = scriptForProject("electron")

	lazy val clientScript = scriptForProject("client")
	lazy val clientDependencies = dependenciesForProject("client")
}
