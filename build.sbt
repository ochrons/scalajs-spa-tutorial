import sbt.Keys._
import sbt.Project.projectToRef

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := Settings.versions.scala,
    sourceDirectory in Assets := baseDirectory.value / "src" / "main" / "assets",
    LessKeys.compress in Assets := true,
    libraryDependencies ++= Settings.sharedDependencies.value
  )
  // .jsConfigure(_ enablePlugins ScalaJSPlay)
  // set up settings specific to the JS project
  .jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val sharedJVM = shared.jvm

lazy val sharedJS = shared.js

// use eliding to drop some debug code in production build
lazy val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")

// instantiate the JS project for SBT with some additional settings
lazy val client: Project = (project in file("client")).settings(
  name := "client",
  version := Settings.version,
  scalaVersion := Settings.versions.scala,
  // by default we do development build
  elideOptions := Seq(),
  scalacOptions ++= Settings.scalacOptions,
  scalacOptions ++= elideOptions.value,
  unmanagedSourceDirectories in Compile := Seq((scalaSource in Compile).value),
  libraryDependencies ++= Settings.scalajsDependencies.value,
  jsDependencies ++= Settings.jsDependencies.value,
  // RuntimeDOM is needed for tests
  jsDependencies += RuntimeDOM % "test",
  // use Scala.js provided launcher code to start the client app
  persistLauncher := true,
  persistLauncher in Test := false,
  sourceMapsDirectories += sharedJS.base / "..",
  // yes, we want to package JS dependencies
  skip in packageJSDependencies := false,
  // use uTest framework for tests
  testFrameworks += new TestFramework("utest.runner.Framework")
)
  .enablePlugins(SbtWeb, ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)

// Client projects (just one)
lazy val clients = Seq(client)

// instantiate the JVM project for SBT with some additional settings
lazy val server = (project in file("server")).settings(
  name := "server",
  version := Settings.version,
  scalaVersion := Settings.versions.scala,
  libraryDependencies ++= Settings.jvmDependencies.value,
  scalaJSProjects := clients,
  // make server depend on changes in the client
  compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in(client, Compile)),
  dist <<= dist dependsOn (fullOptJS in(client, Compile)),
  stage <<= stage dependsOn (fullOptJS in(client, Compile)),
  pipelineStages := Seq(scalaJSProd),
  LessKeys.compress in Assets  := true,
  scalacOptions ++= Settings.scalacOptions,
  // set environment variables in the execute scripts
  NativePackagerKeys.batScriptExtraDefines += "set PRODUCTION_MODE=true",
  NativePackagerKeys.bashScriptExtraDefines += "export PRODUCTION_MODE=true"
).settings(
    // ask scalajs project to put its outputs in to the public web dir
    Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
      crossTarget in(client, Compile, packageJSKey) := (WebKeys.public in Assets).value
    }
  ).enablePlugins(SbtWeb, PlayScala).disablePlugins(PlayLayoutPlugin)
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)

// Command for building a release
lazy val ReleaseCmd = Command.command("release") {
  state => "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
    "client/test" ::
    "client/fullOptJS" ::
    "server/test" ::
    "server/dist" ::
    "set elideOptions in client := Seq()" ::
    state
}

// root project aggregating the JS and JVM projects
lazy val root = project.in(file(".")).
  aggregate(client, server).
  settings(
    commands += ReleaseCmd,
    publish := {},
    publishLocal := {}
  )

