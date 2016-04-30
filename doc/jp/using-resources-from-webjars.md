# Using resources from WebJars

[WebJars](http://www.webjars.org) are a wonderful way to simplify inclusion of external resources such as JavaScript libraries and CSS definitions into
your own project. Instead of downloading JS/CSS packages like Bootstrap and extracting them within your project (or referring to external CDN
served resources), you can just add a dependency to the appropriate WebJar and you're all set!

## WebJar JavaScript

Scala.js SBT plugin offers a [nice and convenient way](http://www.scala-js.org/doc/sbt/depending.html) for extracting JavaScript sources from various
WebJars and concatenating them into a single JavaScript file that you can then refer to in your `index.html`. In the tutorial project this means following
configuration in the `build.scala` file:

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

This will produce a file named `client-jsdeps.js` containing all those JavaScript files combined. In production build, a minimized version of each
JavaScript file is selected.

## WebJar CSS/LESS

For extracting CSS files from WebJars you could use the method described below, but there is bit more convenient method that gives you [LESS](http://lesscss.org/)
processing as a bonus. First we'll need to add the [sbt-less](https://github.com/sbt/sbt-less) plugin into our `plugins.sbt`

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
```

The server project automatically enables the `sbt-web` and `sbt-less` plugins because it uses the `PlayScala` plugin.

We'll be storing LESS files under `src/main/assets/stylesheets` to keep them separated from directly copied resources.

```scala
LessKeys.compress in Assets  := true,
```
This tells the LESS compiler to minify the produced CSS.

Next step is to create a `main.less` (yes, it has to be named exactly that) with references to CSS/LESS files inside the WebJars.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
```

In this case we just import Bootstrap and Font Awesome LESS files because all other CSS styles are defined using ScalaCSS. Depending on the WebJar, 
it may or may not contain LESS files in addition to the CSS file. With the LESS files you can easily 
[configure the library](http://getbootstrap.com/css/#less) to your liking by defining CSS variables in your `main.less` file.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
@brand-danger:  #00534f;
```

## WebJar resource files

Sometimes WebJars contain other useful resources, such as the font files for Font Awesome in our case. Just including the WebJar as a dependency will provide
us the extracted contents and it can be used directly.
