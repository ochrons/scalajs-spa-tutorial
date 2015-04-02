# Production build

So far we have been interested in having a nice and fast development cycle, without worrying about optimization. For your production release you'll want
to have a very optimized and small code-base to make the application perform well for the end user. To build an optimized version of the application
you just need to use `fullOptJS` instead of `fastOptJS`. This will produce a JavaScript file with `-opt.js` extension, so the server must know to serve
this file instead of the non-optimized version.

## Configuring between development and production

To achieve this, we need to add a configuration parameter that can be affected from outside the application. Typesafe provides a nice
[config library](https://github.com/typesafehub/config) for application configuration data so we'll be using it. The library automatically reads
`reference.conf` and `application.conf` (amongst a few others) from resources, so all you need to is to add an `application.conf` under `resources`.
Our configuration is extremely simple:

```yaml
spatutorial {
  productionMode = false
  productionMode = ${?PRODUCTION_MODE}
}
```

These configs can be overridden by having a separate `application.conf` in your built application conf-directory, but we are using a system
environment variable to override the single setting. By default `productionMode` is `false` but if the env variable `PRODUCTION_MODE` is set,
it will override the default.

`MainApp` loads this configuration into a `Config` object.

```scala
object Config {
  val c = ConfigFactory.load().getConfig("spatutorial")

  val productionMode = c.getBoolean("productionMode")
}
```

The `productionMode` parameter is only used to determine which HTML file to serve.

```scala
  if(Config.productionMode)
    getFromResource("web/index-full.html")
  else
    getFromResource("web/index.html")
```

## Using optimized versions of JS libraries

Typically JS libraries provided in WebJars come in two variants: normal and minified (`.min.js`). The latter is highly compressed and often also optimized
version of the more verbose normal version. For example debug-prints and development time checks have been removed. Therefore it makes sense to use these
pre-packaged minified versions instead of running the minification process yourself.

We need to define a separate list of JS dependencies for the production build, using the `.min.js` versions:

```scala
val jsDependenciesProduction = Def.setting(Seq(
  "org.webjars" % "react" % versions.react / "react-with-addons.min.js" commonJSName "React",
  "org.webjars" % "jquery" % versions.jQuery / "jquery.min.js",
  "org.webjars" % "bootstrap" % versions.bootstrap / "bootstrap.min.js" dependsOn "jquery.min.js",
  "org.webjars" % "chartjs" % versions.chartjs / "Chart.min.js",
  "org.webjars" % "log4javascript" % versions.log4js / "js/log4javascript.js"
))
```

Because packaging the JS libs is a different task in SBT (it doesn't relate to `fastOptJS` or `fullOptJS`) we need another way to tell SBT to use these
dependencies instead of the normal ones. For that we'll define a `settingKey`

```scala
  val productionBuild = settingKey[Boolean]("Build for production")
```

and set it in the JS project

```scala
.jsSettings(
  productionBuild := false,
  jsDependencies ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value},
```

To do a production build, you'll need to override the setting on SBT command line:

```
set productionBuild in js := true
```

If you now run `packageJSDependencies` it will use the minified versions and build a smaller `-jsdeps.js` file.

## Packaging an application

Running your app from SBT is fine for development but not so great in production. To build an application package with all the JAR libraries included,
we can use the [`sbt-native-packager`](https://github.com/sbt/sbt-native-packager) plugin.

```scala
// in plugins.sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0-RC1")

// in build.scala
jvm.enablePlugins(JavaAppPackaging)
```

This is all you need to enable the `stage` command in SBT (under the JVM project), which will build and package the application for you in the
`target/universal/stage` directory. You may run the application with `bin/scalajs-spa` (or `bin\scalajs-spa.bat` in Windows).

We can configure the packager to set the `PRODUCTION_BUILD` environment variable automatically in the script with the following settings

```scala
NativePackagerKeys.batScriptExtraDefines += "set PRODUCTION_MODE=true",
NativePackagerKeys.bashScriptExtraDefines += "export PRODUCTION_MODE=true",
```

## Automating the release build

Even though we have all the pieces to build and package the application, it can be quite tedious to run several SBT commands one after another
to get everything done. That's what computers are really good at, so let's build a special `Command` to do the release.

```scala
val ReleaseCmd = Command.command("release") {
  state => "set productionBuild in js := true" ::
    "set elideOptions in js := Seq(\"-Xelide-below\", \"WARNING\")" ::
    "sharedProjectJS/test" ::
    "sharedProjectJS/fullOptJS" ::
    "sharedProjectJS/packageJSDependencies" ::
    "sharedProjectJVM/test" ::
    "sharedProjectJVM/stage" ::
    "set productionBuild in js := false" ::
    "set elideOptions in js := Seq()" ::
    state
}
```

and enable it in the root project

```scala
  commands += ReleaseCmd,
```

With this command, you can just execute `release` under the `root` and SBT will run all those individual commands to build
your application package.
