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

lazy val commonScalaJsSettings: Seq[Def.Setting[_]] = Seq(
	scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)

lazy val server = (project in file("server"))
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			guice,
			"com.vmunier" %% "scalajs-scripts" % "1.1.1",
			"org.mindrot" % "jbcrypt" % "0.4",
			"com.typesafe.slick" %% "slick" % "3.2.1",
			"com.typesafe.play" %% "play-slick" % "3.0.1",
			"mysql" % "mysql-connector-java" % "5.1.23"
		),
		scalaJSProjects := Seq(client, electron),
		pipelineStages in Assets := Seq(scalaJSPipeline),
		pipelineStages := Seq(digest, gzip),
		compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
		includeFilter in (Assets, LessKeys.less) := "*.less",
		excludeFilter in (Assets, LessKeys.less) := "_*.less"
	)
	.enablePlugins(PlayScala, DockerPlugin)
	.dependsOn(sharedJvm)

lazy val client = (project in file("client"))
	.settings(
		commonSettings,
		commonScalaJsSettings,
		//scalaJSUseMainModuleInitializer := true,
		libraryDependencies ++= Seq(
			"org.scala-js" %%% "scalajs-dom" % "0.9.3"
		)
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs, facades)

lazy val electron = (project in file("electron"))
	.settings(
		commonSettings,
		commonScalaJsSettings,
		scalaJSUseMainModuleInitializer := true
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs, facades)

lazy val facades = (project in file("facades"))
	.settings(
		commonSettings,
		commonScalaJsSettings,
		libraryDependencies ++= Seq(
			"org.scala-js" %%% "scalajs-dom" % "0.9.3"
		)
	)
	.enablePlugins(ScalaJSPlugin)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
	.settings(
		name := "shared",
		commonSettings,
		libraryDependencies ++= Seq(
		)
	)
	.jsSettings(commonScalaJsSettings)
	.jvmSettings(
		libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "0.6.19" % "provided"
	)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

