

name := "wfit"

version in ThisBuild := "latest-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.3"

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
		"-opt:l:method"
	)
)

lazy val commonScalaJsSettings: Seq[Def.Setting[_]] = Seq(
	scalacOptions += "-P:scalajs:sjsDefinedByDefault"
)

lazy val root = project.in(file(".")).aggregate(server).settings(
	run := { (run in server in Compile).evaluated }
)

lazy val server = (project in file("server"))
	.settings(
		name := "wfit-server",
		commonSettings,
		libraryDependencies ++= Seq(
			guice,
			ehcache,
			ws,
			"com.vmunier" %% "scalajs-scripts" % "1.1.1",
			"org.mindrot" % "jbcrypt" % "0.4",
			"com.typesafe.slick" %% "slick" % "3.2.1",
			"com.typesafe.play" %% "play-slick" % "3.0.2",
			"org.mariadb.jdbc" % "mariadb-java-client" % "2.1.2",
			"org.ocpsoft.prettytime" % "prettytime" % "4.0.1.Final"
		),
		scalaJSProjects := Seq(client, electron),
		pipelineStages in Assets := Seq(scalaJSPipeline),
		pipelineStages := Seq(digest, gzip),
		compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
		includeFilter in (Assets, LessKeys.less) := "*.less",
		excludeFilter in (Assets, LessKeys.less) := "_*.less",
		TwirlKeys.templateImports ++= Seq(
			"_root_.controllers.base._",
			"_root_.utils._",
			"_root_.utils.Timeago.Implicitly"
		)
	)
	.enablePlugins(PlayScala, DockerPlugin)
	.dependsOn(sharedJvm)

lazy val client = (project in file("client"))
	.settings(
		commonSettings,
		commonScalaJsSettings,
		//scalaJSUseMainModuleInitializer := true,
		libraryDependencies ++= Seq(
			"org.scala-js" %%% "scalajs-dom" % "0.9.3",
			"com.typesafe.play" %%% "play-json" % "2.6.3",
			"in.nvilla" %%% "monadic-html" % "0.3.2",
			"org.webjars.npm" % "dexie" % "1.4.1"
		),
		jsDependencies ++= Seq(
			"org.webjars.npm" % "dexie" % "1.4.1" / "1.4.1/dist/dexie.min.js"
		),
		skip in packageJSDependencies := false
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb, JSDependenciesPlugin)
	.dependsOn(sharedJs)

lazy val electron = (project in file("electron"))
	.settings(
		commonSettings,
		commonScalaJsSettings,
		scalaJSUseMainModuleInitializer := true
	)
	.enablePlugins(ScalaJSPlugin, ScalaJSWeb)
	.dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Full) in file("shared"))
	.settings(
		name := "shared",
		commonSettings,
		libraryDependencies ++= Seq(
			"com.typesafe.play" %%% "play-json" % "2.6.3"
		)
	)
	.jsSettings(
		commonScalaJsSettings,
		libraryDependencies ++= Seq(
			"io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M12",
			"org.scala-js" %%% "scalajs-dom" % "0.9.3"
		)
	)
	.jvmSettings(
		libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "0.6.19" % "provided"
	)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
