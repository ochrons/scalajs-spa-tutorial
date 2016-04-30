# Production build

So far we have been interested in having a nice and fast development cycle, without worrying about optimization. For your production release you'll want
to have a very optimized and small code-base to make the application perform well for the end user. To build an optimized version of the client application
you just need to use `fullOptJS` instead of `fastOptJS`. This will produce a JavaScript file with `-opt.js` extension, so the server must know to serve
this file instead of the non-optimized version. We are using @vmunier's `play-scalajs-scripts` to do this automatically within our HTML template.

## Using optimized versions of JS libraries

Typically JS libraries provided in WebJars come in two variants: normal and minified (`.min.js`). The latter is highly compressed and often also optimized
version of the more verbose normal version. For example debug-prints and development time checks have been removed. Therefore it makes sense to use these
pre-packaged minified versions instead of running the minification process yourself.

We need to define a separate JS dependencies for the production build, using the `minified` keyword:

```scala
/** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
val jsDependencies = Def.setting(Seq(
  "org.webjars.bower" % "react" % versions.react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % versions.react / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
  "org.webjars" % "jquery" % versions.jQuery / "jquery.js" minified "jquery.min.js",
  "org.webjars" % "bootstrap" % versions.bootstrap / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
  "org.webjars" % "chartjs" % versions.chartjs / "Chart.js" minified "Chart.min.js",
  "org.webjars" % "log4javascript" % versions.log4js / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js"
))
```

Scala.js plugin will then automatically select the minified version when running `fullOptJS`.

## Packaging an application

Play makes it very easy to package your application with the `dist` command. The plugins make sure client is complied with `fullOptJS` and all
relevant files end up in the distribution assets.

## Automating the release build

Even though we have all the pieces to build and package the application, it can be quite tedious to run several SBT commands one after another
to get everything done. That's what computers are really good at, so let's build a special `Command` to do the release. Here we also set
the `elideOptions` for client to get rid of any debug code.

```scala
lazy val ReleaseCmd = Command.command("release") {
  state => "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
    "client/clean" ::
    "client/test" ::
    "server/clean" ::
    "server/test" ::
    "server/dist" ::
    "set elideOptions in client := Seq()" ::
    state
}
}
```

and enable it in the `server` project

```scala
  commands += ReleaseCmd,
```

With this command, you can just execute `release` and SBT will run all those individual commands to build your application package.
