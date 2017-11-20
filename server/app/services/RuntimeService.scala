package services

import base.{AppComponents, AppService}
import controllers.routes
import javax.inject.{Inject, Singleton}
import models.UUID
import org.apache.commons.codec.digest.DigestUtils

@Singleton
class RuntimeService @Inject()(cc: AppComponents) extends AppService(cc) {
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

	lazy val bootstrapScript: Option[String] = scriptForProject("wfit-electron")
	lazy val launcherDigest: String = DigestUtils.sha1Hex(bootstrapScript.getOrElse(""))

	lazy val clientScript: Option[String] = scriptForProject("wfit-client")
	lazy val clientDependencies: Option[String] = dependenciesForProject("wfit-client")
}
