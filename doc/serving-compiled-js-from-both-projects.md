# Serving compiled JS from both projects

Scala.js SBT [Workbench plugin](https://github.com/lihaoyi/workbench) enables quick development by serving client files straight from SBT
without the need for your own server. The relevant part in `build.scala` is

```scala
// define where the JS-only application will be hosted by the Workbench plugin
localUrl :=("localhost", 13131),
refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile),
bootSnippet := "SPAMain().main();"
```

To use the Workbench plugin, point your browser to http://localhost:13131/jvm/target/scala-2.11/classes/web/index.html. Whenever you run `fastOptJS` the plugin will automatically refresh the browser with an updated version. How's that for fast turn-around times!

But since Scala.js compile output is stored under the JS project by default, how can we serve it from the JVM project? The solution is to
instruct Scala.js to save its output in a specific directory under the JVM project and then make a copy back to the JS side. For this you need
a bit more SBT code to make it work

```scala
// configure a specific directory for scalajs output
val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

// make all JS builds use the output dir defined later
lazy val js2jvmSettings = Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
  crossTarget in(spaJS, Compile, packageJSKey) := scalajsOutputDir.value
}

// instantiate the JS project for SBT with some additional settings
lazy val spaJS: Project = spa.js.settings(
  fastOptJS in Compile := {
    // make a copy of the produced JS-file (and source maps) under the spaJS project as well,
    // because the original goes under the spaJVM project
    // NOTE: this is only done for fastOptJS, not for fullOptJS
    val base = (fastOptJS in Compile).value
    IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / base.data.getName)
    IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / (base.data.getName + ".map"))
    base
  }
)

// instantiate the JVM project for SBT with some additional settings
lazy val spaJVM: Project = spa.jvm.settings(js2jvmSettings: _*).settings(
  // scala.js output is directed under "web/js" dir in the spaJVM project
  scalajsOutputDir := (classDirectory in Compile).value / "web" / "js",
  // reStart depends on running fastOptJS on the JS project
  Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in(js, Compile))
)
```

