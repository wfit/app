import scala.languageFeature.experimental.macros

name := "wfit"
version := "latest"

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
	scalaVersion := "2.12.3",
	scalacOptions ++= Seq(
		//"-Xlog-implicits",
		"-deprecation",
		"-unchecked",
		"-Xfatal-warnings",
		"-feature",
		"-language:implicitConversions",
		"-language:reflectiveCalls",
		"-language:higherKinds"
	)
)

lazy val server = (project in file("server"))
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			guice,
			"com.vmunier" %% "scalajs-scripts" % "1.1.1"
		),
		scalaJSProjects := Seq(client, electron),
		pipelineStages in Assets := Seq(scalaJSPipeline),
		pipelineStages := Seq(digest, gzip),
		compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value
	)
	.enablePlugins(PlayScala, DockerPlugin)
	.dependsOn(sharedJvm)

lazy val client = (project in file("client"))
	.settings(
		commonSettings,
		//scalaJSUseMainModuleInitializer := true,
		libraryDependencies ++= Seq(
			"org.scala-js" %%% "scalajs-dom" % "0.9.3"
		)
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs)

lazy val electron = (project in file("electron"))
	.settings(
		commonSettings,
		scalaJSUseMainModuleInitializer := true
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
	.settings(
		name := "shared",
		commonSettings,
		libraryDependencies ++= Seq(
		)
	)
	.jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

