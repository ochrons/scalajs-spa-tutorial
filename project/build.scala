import com.lihaoyi.workbench.Plugin._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object ApplicationBuild extends Build {
  // root project aggregating the JS and JVM projects
  lazy val root = project.in(file(".")).
    aggregate(js, jvm).
    settings(
      publish := {},
      publishLocal := {}
    )

  val sharedSrcDir = "shared"

  // a special crossProject for configuring a JS/JVM/shared structure
  lazy val sharedProject = crossProject.in(file(".")).
    settings(
      name         := "scalajs-spa",
      version      := "0.1.3",
      scalaVersion := "2.11.5",
      scalacOptions ++= Seq(
        "-Xlint",
        "-unchecked",
        "-deprecation",
        "-feature"
      ),
    
      libraryDependencies ++= Seq(
        // these dependencies are shared between JS and JVM projects
        // the special %%% function selects the correct version for each project
        "com.lihaoyi"   %%% "autowire"  % "0.2.4",
        "com.lihaoyi"   %%% "upickle"   % "0.2.6"
      )
    ).
    
    // set up settings specific to the JVM project
    jvmSettings(Revolver.settings: _*).
    jvmSettings(
      libraryDependencies ++= Seq(
        // dependencies only used by the JVM project
        "io.spray"          %% "spray-can"      % "1.3.2",
        "io.spray"          %% "spray-routing"  % "1.3.2",
        "com.typesafe.akka" %% "akka-actor"     % "2.3.6"
      ),
      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test    += file(".") / sharedSrcDir / "src" / "test" / "resources",
      
      // set some basic options when running the project with Revolver
      javaOptions in Revolver.reStart ++= Seq("-Xmx1G"),
      
      // configure a specific port for debugging, so you can easily debug multiple projects at the same time if necessary
      Revolver.enableDebugging(port = 5111, suspend = false)
    ).
    // set up settings specific to the JS project
    jsSettings(workbenchSettings: _*).
    jsSettings(
      libraryDependencies ++= Seq(
        // dependencies only used by the JS project (note the use of %%% instead of %%)
        "org.scala-js"                      %%% "scalajs-dom" % "0.8.0",
        "com.github.japgolly.scalajs-react" %%% "core"        % "0.8.0",
        "com.github.japgolly.scalajs-react" %%% "extra"       % "0.8.0",
        "com.lihaoyi"                       %%% "scalarx"     % "0.2.7",
        "com.lihaoyi"                       %%% "utest"       % "0.3.0"
      ),
      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test    += file(".") / sharedSrcDir / "src" / "test" / "resources",
      
      // use uTest framework for tests
      testFrameworks += new TestFramework("utest.runner.Framework"),
      
      // define where the JS-only application will be hosted by the Workbench plugin
      localUrl        :=("localhost", 13131),
      refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile),
      bootSnippet     := "SPAMain().main();"
    )

  // configure a specific directory for scalajs output
  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  // make all JS builds use the output dir defined later
  lazy val js2jvmSettings = Seq(packageScalaJSLauncher, fastOptJS, fullOptJS) map { packageJSKey =>
    crossTarget in(js, Compile, packageJSKey) := scalajsOutputDir.value
  }

  // instantiate the JS project for SBT with some additional settings
  lazy val js: Project = sharedProject.js.settings(
    fastOptJS in Compile := {
      // make a copy of the produced JS-file (and source maps) under the js project as well,
      // because the original goes under the jvm project
      // NOTE: this is only done for fastOptJS, not for fullOptJS
      val base = (fastOptJS in Compile).value
      IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / base.data.getName)
      IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / (base.data.getName + ".map"))
      base
    }
  )

  // instantiate the JVM project for SBT with some additional settings
  lazy val jvm: Project = sharedProject.jvm.settings(js2jvmSettings: _*).settings(
    // scala.js output is directed under "web/js" dir in the jvm project
    scalajsOutputDir := (classDirectory in Compile).value / "web" / "js",
    
    // compile depends on running fastOptJS on the JS project
    compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in(js, Compile))
  )
}
