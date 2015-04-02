# Sharing web resources between JS and JVM projects

In addition to the compiled Scala.js code, you need other resources such as the `index.html` some CSS and JS files for your application to work.
You should store these under the `shared` project resources and let the JS/JVM project know about these extra resource directories.

```scala
val sharedSrcDir = "shared"

// copy resources from the "shared" project
unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",
```

