import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

name := "wfit"

version in ThisBuild := "latest-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.6"

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
	scalacOptions ++= Seq(
		//"-Xlog-implicits",
		"-deprecation",
		"-unchecked",
		"-Xfatal-warnings",
		"-feature",
		"-language:implicitConversions",
		"-language:reflectiveCalls",
		"-language:higherKinds",
		//"-opt:l:method",
	),
	sources in (Compile, doc) := Seq.empty,
	publishArtifact in (Compile, packageDoc) := false,
	publishArtifact in (Compile, packageSrc) := false,
)

lazy val commonScalaJsSettings: Seq[Def.Setting[_]] = Seq(
	scalacOptions += "-P:scalajs:sjsDefinedByDefault",
)

lazy val root = project.in(file("."))
	.aggregate(
		server,
		client,
		electron,
		sharedJvm,
		sharedJs,
	).settings(
		run := { (run in server in Compile).evaluated },
	)

lazy val server = (project in file("server"))
	.settings(
		name := "wfit-server",
		commonSettings,
		libraryDependencies ++= Seq(
			guice,
			ehcache,
			ws,
			"com.vmunier" %% "scalajs-scripts" % "1.1.2",
			"org.mindrot" % "jbcrypt" % "0.4",
			"com.typesafe.slick" %% "slick" % "3.2.3",
			"com.typesafe.play" %% "play-slick" % "3.0.3",
			"org.mariadb.jdbc" % "mariadb-java-client" % "2.2.6",
			"org.ocpsoft.prettytime" % "prettytime" % "4.0.2.Final",
			"com.google.javascript" % "closure-compiler" % "v20180506",
		),
		scalaJSProjects := Seq(client, electron),
		pipelineStages in Assets := Seq(scalaJSPipeline),
		pipelineStages := Seq(digest, gzip),
		compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
		DigestKeys.indexPath := Some("javascripts/versioned.js"),
		DigestKeys.indexWriter ~= { writer => index => s"var versioned = ${ writer(index) };" },
		TwirlKeys.templateImports ++= Seq(
			"_root_.base._",
			"_root_.utils._",
			"_root_.models.UUID",
		),
		PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.polling(500),
		fork := true,
	)
	.enablePlugins(PlayScala, DockerPlugin)
	.dependsOn(sharedJvm)

lazy val client = (project in file("client"))
	.settings(
		name := "wfit-client",
		commonSettings,
		commonScalaJsSettings,
		scalaJSUseMainModuleInitializer := true,
		libraryDependencies ++= Seq(
			"in.nvilla" %%% "monadic-html" % "0.3.2",
		),
		jsDependencies ++= Seq(
		),
		skip in packageJSDependencies := false,
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb, JSDependenciesPlugin)
	.dependsOn(sharedJs)

lazy val electron = (project in file("electron"))
	.settings(
		name := "wfit-electron",
		commonSettings,
		commonScalaJsSettings,
		scalaJSUseMainModuleInitializer := true,
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs)

lazy val shared = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full) in file("shared"))
	.settings(
		name := "shared",
		commonSettings,
		libraryDependencies ++= Seq(
			"com.typesafe.play" %%% "play-json" % "2.6.10",
		)
	)
	.jsSettings(
		commonScalaJsSettings,
		libraryDependencies ++= Seq(
			"io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M13",
			"org.scala-js" %%% "scalajs-dom" % "0.9.6",
//			"org.akka-js" %%% "akkajsactor" % "1.2.5.15",
//			"org.akka-js" %%% "akkajsactorstream" % "1.2.5.15",
		)
	)
	.jvmSettings(
		libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "0.6.24" % "provided",
	)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
