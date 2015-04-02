# Debugging

Scala.js supports source maps, making debugging your code in the browser quite easy. You can set breakpoints in the original source-code
and inspect local variables etc. just like in a real IDE debugger. See the documentation for your browser developer tools for more information.

## Setting source maps

By default Scala.js generates source-maps using local file paths, but for some reason this doesn't work on (my) Chrome, even though it works just
fine in Firefox. To get around this limitation, we'll modify the source-map to use relative URLs and serve source files directly from our
web server. To remap source paths, use following configuration in the build file.

```scala
scalacOptions ++= Seq({
  val a = js.base.toURI.toString.replaceFirst("[^/]+/?$", "")
  s"-P:scalajs:mapSourceURI:$a->/srcmaps/"
})
```

This will replace the local file path with a relative URL starting with `/srcmaps/` which we will then use on our server to provide the source files.
In the server code you'll need to add following:

```scala
pathPrefix("srcmaps") {
  if(!Config.productionMode)
    getFromDirectory("../")
  else
    complete(StatusCodes.NotFound)
}
```

Note how serving source files is only enabled in development builds. As the server is running in the `jvm` directory, the relative path `../` will point
to the directory directly above it, which will allow access to the source file under `js/src`. If your project configuration is different, you may
need to change this.

## Actual debugging

When running the application, you can access the sources through the developer tools window as shown below.

![debug sources](images/debug1.png?raw=true)

You can set breakpoints and investigate variables to see what's going on in your code. Some of the variable names have funny extensions like `completed$1`
but this is just due to name mangling by Scala. Below you can see how the debugger has hit a breakpoint and the local variables are displayed automatically.

![breakpoints](images/debug2.png?raw=true)

You might also want to install Facebook [React DevTools](https://chrome.google.com/webstore/detail/react-developer-tools/fmkadmapgofadopljbjfkapdkoienihi) into
your Chrome browser to help visualize active React components in the DevTools window.

