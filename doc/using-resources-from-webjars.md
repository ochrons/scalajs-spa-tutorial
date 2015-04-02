# Using resources from WebJars

[WebJars](http://www.webjars.org) are a wonderful way to simplify inclusion of external resources such as JavaScript libraries and CSS definitions into
your own project. Instead of downloading JS/CSS packages like Bootstrap and extracting them within your project (or referring to external CDN
served resources), you can just add a dependency to the appropriate WebJar and you're all set! Well, except you aren't, unless you were using something
like Play, which this tutorial isn't using. So how do you actually get those resources out of a WebJar and into your web resource directory? The process
is a bit different for JavaScript, CSS (LESS) and other resources files.

## WebJar JavaScript

Scala.js SBT plugin offers a [nice and convenient way](http://www.scala-js.org/doc/sbt/depending.html) for extracting JavaScript sources from various
WebJars and concatenating them into a single JavaScript file that you can then refer to in your `index.html`. In the tutorial project this means following
configuration in the `build.scala` file:

```scala
/** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
val jsDependencies = Def.setting(Seq(
  "org.webjars" % "react" % versions.react / "react-with-addons.js" commonJSName "React",
  "org.webjars" % "jquery" % versions.jQuery / "jquery.js",
  "org.webjars" % "bootstrap" % versions.bootstrap / "bootstrap.js" dependsOn "jquery.js",
  "org.webjars" % "chartjs" % versions.chartjs / "Chart.js",
  "org.webjars" % "log4javascript" % versions.log4js / "js/log4javascript_uncompressed.js"
))

.jsSettings(
      jsDependencies      ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value},
      jsDependencies      +=  RuntimeDOM % "test",
      skip in packageJSDependencies := false,
```

This will produce a file named `web/js/scalajs-spa-jsdeps.js` containing all those JavaScript files combined.

## WebJar CSS/LESS

For extracting CSS files from WebJars you could use the method described below, but there is bit more convenient method that gives you [LESS](http://lesscss.org/)
processing as a bonus. First we'll need to add the [sbt-less](https://github.com/sbt/sbt-less) plugin into our `plugins.sbt`

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
```

The projects also need to enable the `sbt-web` plugin for this to work.

```scala
jvm.enablePlugins(SbtWeb)
js.enablePlugins(SbtWeb)
```

Because `sbt-less` and other `sbt-web` plugins are heavily related to the Play framework, we'll need to do some additional configuration to make it work with
our simple tutorial. We'll be storing LESS files under `src/main/assets` to keep them separated from directly copied resources.

```scala
import com.typesafe.sbt.less.Import._

sourceDirectory in Assets    := baseDirectory.value / "src" / "main" / "assets",
LessKeys.compress in Assets  := true,
```
The last line tells the LESS compiler to minify the produced CSS.

Next step is to create a `main.less` (yes, it has to be named exactly that) with references to CSS/LESS files inside the WebJars.
Note that due to a [bug](https://github.com/sbt/sbt-less/issues/30) in `sbt-web` we cannot put the `mail.less` under the `shared` project where it belongs, but
must duplicate it to both JS and JVM projects.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";

body {
    padding-top: 50px;
}

.label-as-badge {
    border-radius: 1em;
}
```

In this case we import Bootstrap and Font Awesome LESS files. Depending on the WebJar, it may or may not contain LESS files in addition to the CSS file. With
the LESS files you can easily [configure the library](http://getbootstrap.com/css/#less) to your liking by defining CSS variables in your `main.less` file.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
@brand-danger:  #00534f;
```

The problem with the `sbt-less` plugin is that it saves the compiled CSS file into wrong directory (since we are not using Play), so we need to copy it
to the correct location. This will be covered in the next section.

## WebJar resource files

Sometimes WebJars contain other useful resources, such as the font files for Font Awesome in our case. Just including the WebJar as a dependency will provide
us the extracted contents, but it's in the wrong place and doesn't get bundled into the final package. So we will need to copy relevant files where
we want them.

To achieve this, we'll define our very own SBT task to do the job.
```scala
val copyWebJarResources = TaskKey[Unit]("Copy resources from WebJars")

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
}
```

Without going into the SBT details, this task copies the generated CSS file (and its sourcemap) into `web/stylesheets`. It also copies all files found
under the Font Awesome `fonts` folder into the appropriate place under our project. If you need to copy other resources, you can use the same code just by
modifying the path definitions.

Finally we need to tell SBT to actually run this task at the appropriate moment. Since we need the compiled CSS output, it has to happen after the fact,
but it also has to happen before the application is finally put together so that the files are in place for packaging or running the app.

```scala
// run the copy after compile/assets but before managed resources
copyWebJarResources <<= copyWebJarResources dependsOn (compile in Compile, assets in Compile),
managedResources in Compile <<= (managedResources in Compile) dependsOn copyWebJarResources
```

NOTE! This whole procedure is IMHO a dirty hack, but couldn't find a better solution. If you do know a better way to do this, please create an issue.

