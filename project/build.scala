import com.lihaoyi.workbench.Plugin._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.less.Import._
import com.typesafe.sbt.packager.archetypes._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

/**
 * Application settings. Configure the build for your application here.
 * You normally don't have to touch the actual build definition after this.
 */
object Settings {
  /** The name of your application */
  val name = "scalajs-spa"

  /** The version of your application */
  val version = "0.1.6"

  /** Options for the scala compiler */
  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )

  /** Set some basic options when running the project with Revolver */
  val jvmRuntimeOptions = Seq(
    "-Xmx1G"
  )

  /** Declare global dependency versions here to avoid mismatches in multi part dependencies */
  object versions {
    val scala = "2.11.5"
    val scalajsReact = "0.8.1"
  }

  /**
   * These dependencies are shared between JS and JVM projects
   * the special %%% function selects the correct version for each project
   */
  val sharedDependencies = Def.setting(Seq(
    "com.lihaoyi" %%% "autowire" % "0.2.4",
    "com.lihaoyi" %%% "upickle" % "0.2.6",
    "org.webjars" % "font-awesome" % "4.3.0-1" % Compile,
    "org.webjars" % "bootstrap" % "3.3.2" % Compile
  ))

  /** Dependencies only used by the JVM project */
  val jvmDependencies = Def.setting(Seq(
    "io.spray" %% "spray-can" % "1.3.2",
    "io.spray" %% "spray-routing" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % "2.3.6"
  ))

  /** Dependencies only used by the JS project (note the use of %%% instead of %%) */
  val scalajsDependencies = Def.setting(Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % versions.scalajsReact,
    "com.github.japgolly.scalajs-react" %%% "extra" % versions.scalajsReact,
    "com.lihaoyi" %%% "scalarx" % "0.2.7",
    "com.lihaoyi" %%% "utest" % "0.3.0"
  ))

  /** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
  val jsDependencies = Def.setting(Seq(
    "org.webjars" % "react" % "0.12.1" / "react-with-addons.js" commonJSName "React",
    "org.webjars" % "jquery" % "1.11.1" / "jquery.js",
    "org.webjars" % "bootstrap" % "3.3.2" / "bootstrap.js" dependsOn "jquery.js",
    "org.webjars" % "chartjs" % "1.0.1" / "Chart.js"
  ))
  /** Same dependecies, but for production build, using minified versions */
  val jsDependenciesProduction = Def.setting(Seq(
    "org.webjars" % "react" % "0.12.1" / "react-with-addons.min.js" commonJSName "React",
    "org.webjars" % "jquery" % "1.11.1" / "jquery.min.js",
    "org.webjars" % "bootstrap" % "3.3.2" / "bootstrap.min.js" dependsOn "jquery.min.js",
    "org.webjars" % "chartjs" % "1.0.1" / "Chart.min.js"
  ))
}

object ApplicationBuild extends Build {
  // root project aggregating the JS and JVM projects
  lazy val root = project.in(file(".")).
    aggregate(js, jvm).
    settings(
      name := "SPAtutorial",
      version := Settings.version,
      publish := {},
      publishLocal := {}
    )

  val sharedSrcDir = "shared"

  val productionBuild = settingKey[Boolean]("Build for production")
  val copyWebJarResources = taskKey[Unit]("Copy resources from WebJars")

  // a special crossProject for configuring a JS/JVM/shared structure
  lazy val sharedProject = crossProject.in(file("."))
    .settings(
      name := Settings.name,
      version := Settings.version,
      scalaVersion := Settings.versions.scala,
      scalacOptions ++= Settings.scalacOptions,
      sourceDirectory in Assets := baseDirectory.value / "src" / "main" / "assets",
      LessKeys.compress in Assets := true,
      libraryDependencies ++= Settings.sharedDependencies.value,
      copyWebJarResources := {
        // copy the compiled CSS
        val s = streams.value
        s.log("Copying webjar resources")
        val compiledCss = webTarget.value / "less" / "main" / "stylesheets"
        val targetDir = (classDirectory in Compile).value / "web"
        IO.createDirectory(targetDir / "stylesheets")
        IO.copyDirectory(compiledCss, targetDir / "stylesheets")
        // copy font-awesome fonts from WebJar
        val fonts = (webModuleDirectory in Assets).value / "webjars" / "lib" / "font-awesome" / "fonts"
        IO.createDirectory(targetDir / "fonts")
        IO.copyDirectory(fonts, targetDir / "fonts")
      },
      // run the copy after compile/assets but before managed resources
      copyWebJarResources <<= copyWebJarResources dependsOn(compile in Compile, assets in Compile),
      managedResources in Compile <<= (managedResources in Compile) dependsOn copyWebJarResources
    )

    // set up settings specific to the JVM project
    .jvmSettings(Revolver.settings: _*)
    .jvmSettings(
      libraryDependencies ++= Settings.jvmDependencies.value,

      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",

      javaOptions in Revolver.reStart ++= Settings.jvmRuntimeOptions,

      // configure a specific port for debugging, so you can easily debug multiple projects at the same time if necessary
      Revolver.enableDebugging(port = 5111, suspend = false)
    )

    // set up settings specific to the JS project
    .jsSettings(workbenchSettings: _*)
    .jsSettings(
      libraryDependencies ++= Settings.scalajsDependencies.value,
      // by default we do development build
      productionBuild := false,
      // select JS dependencies according to build setting
      jsDependencies ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value},
      // RuntimeDOM is needed for tests
      jsDependencies += RuntimeDOM % "test",

      // yes, we want to package JS dependencies
      skip in packageJSDependencies := false,

      // copy resources from the "shared" project
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",

      // use uTest framework for tests
      testFrameworks += new TestFramework("utest.runner.Framework"),

      // define where the JS-only application will be hosted by the Workbench plugin
      localUrl :=("localhost", 13131),
      refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile),
      bootSnippet := "SPAMain().main();"
    )

  // configure a specific directory for scalajs output
  val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

  // make all JS builds use the output dir defined later
  lazy val js2jvmSettings = Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
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
    },

    packageJSDependencies in Compile := {
      // make a copy of the produced jsdeps file under the js project as well,
      // because the original goes under the jvm project
      val base = (packageJSDependencies in Compile).value
      IO.copyFile(base, (classDirectory in Compile).value / "web" / "js" / base.getName)
      base
    }
  ).enablePlugins(SbtWeb)

  // Command for building a release
  val ReleaseCmd = Command.command("release") {
    state => "set productionBuild in js := true" ::
      "sharedProjectJS/test" ::
      "sharedProjectJS/fullOptJS" ::
      "sharedProjectJS/packageJSDependencies" ::
      "test" ::
      "stage" ::
      state
  }

  // instantiate the JVM project for SBT with some additional settings
  lazy val jvm: Project = sharedProject.jvm.settings(js2jvmSettings: _*).settings(
    // scala.js output is directed under "web/js" dir in the jvm project
    scalajsOutputDir := (classDirectory in Compile).value / "web" / "js",
    // set environment variables in the execute scripts
    NativePackagerKeys.batScriptExtraDefines += "set PRODUCTION_MODE=true",
    NativePackagerKeys.bashScriptExtraDefines += "export PRODUCTION_MODE=true",
    commands += ReleaseCmd,
    // reStart depends on running fastOptJS on the JS project
    Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in(js, Compile))
  ).enablePlugins(SbtWeb).enablePlugins(JavaAppPackaging)
}
