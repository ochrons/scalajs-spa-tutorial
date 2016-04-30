# SBT build definition

Since Scala.js is quite new and it's been evolving even rather recently, building Scala.js applications with SBT is not as clear as it could
be. Yes, the documentation and tutorials give you the basics, but what if you want something more, like configure a custom directory layout?

The `build.sbt` in this tutorial shows you some typical cases you might run into in your own application. The basic structure of the `build.sbt`
is built on top of the [example](https://github.com/vmunier/play-with-scalajs-example/blob/master/build.sbt) provided by Vincent Munier, author of
the [sbt-play-scalajs](https://github.com/vmunier/sbt-play-scalajs) plugin.

The build defines three separate projects:
* shared
* client
* server

## Shared project

First one is a special Scala.js `CrossProject` that actually contains two projects: one for JS and one for JVM. This `shared` project contains classes, libraries
and resources shared between the client and server. In the context of this tutorial it means just the `Api.scala` trait and `TodoItem.scala` case class.
In a more realistic application you would have your data models etc. defined here.

```scala
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.sharedDependencies.value
  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSPlay)
  .jsSettings(sourceMapsBase := baseDirectory.value / "..")
```

The shared dependencies include libraries used by both client and server such as `autowire` and `boopickle` for client/server communication.
```scala
val sharedDependencies = Def.setting(Seq(
  "com.lihaoyi" %%% "autowire" % versions.autowire,
  "me.chrons" %%% "boopickle" % versions.booPickle,
  "com.lihaoyi" %%% "utest" % versions.uTest
))
```

## Client project

Client is defined as a normal Scala.js project by enabling the `ScalaJSPlugin` on it.

```scala
lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    jsDependencies ++= Settings.jsDependencies.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    persistLauncher := true,
    persistLauncher in Test := false,
    // must specify source maps location because we use pure CrossProject
    sourceMapsDirectories += sharedJS.base / "..",
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)
```

First few settings are normal Scala settings, but let's go through the remaining settings to explain what they do.

```scala
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
```
Eliding is used to remove code that is not needed in the production build, such as debug logging. This setting is empty by default, but is enabled in
the `release` command.

```scala
    jsDependencies ++= Settings.jsDependencies.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
```
The `jsDependencies` defines a set of JavaScript libraries your application depends on. These are also packaged into a single `.js` file for easy
consumptions. For `test` phase we include the `RuntimeDOM` so that Scala.js plugin knows to use PhantomJS instead of the default Rhino to run the tests.
Make sure you have installed [PhantomJS](http://phantomjs.org/) before running the tests.

```scala
    // use Scala.js provided launcher code to start the client app
    persistLauncher := true,
    persistLauncher in Test := false,
```
This setting informs Scala.js plugin to generate a special `launcher.js` file, which is loaded last and invokes your `main` method. Using a launcher keeps
your HTML template clean, as you don't need to specify the `main` function there.

```scala
    // must specify source maps location because we use pure CrossProject
    sourceMapsDirectories += sharedJS.base / "..",
```
Because we are using a pure `CrossProject`, the source map directories have to be manually adjusted to reflect where the source files can be found.

```scala
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
```
Lets SBT know that we are using uTest framework for tests.

```scala
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)
```
We enable both Scala.js and Scala.js-for-Play plugins. Finally the `client` project needs to depend on the `shared` project to get access to shared code
and resources.

## Server project

The server project is a normal Play project with a few twists to make client integration a breeze. Most of the heavy-lifting is done by the `ScalaJSPlay`
plugin, which is automatically included to all projects using `PlayScala` plugin.

```scala
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.jvmDependencies.value,
    commands += ReleaseCmd,
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd),
    // compress CSS
    LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)
```
As with the client project, the first few settings are just normal SBT settings, so let's focus on the more interesting ones.

```scala
    commands += ReleaseCmd,
```
We define a new SBT command `release` to run a sequence of commands to produce a [distribution package](production-build.md).

```scala
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd),
```
Let the plugin know where our client project is and enable Scala.js processing in the pipeline.

```scala
    // compress CSS
    LessKeys.compress in Assets := true,
```
This instructs the `sbt-less` plugin to minify the produced CSS.


```scala
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
```
We use Play, but not its default layout. Instead we prefer the normal SBT layout with `src/main/scala` structure.

```scala
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)
```
Server aggregates the client and also depends on the `shared` project to get access to shared code and resources.
