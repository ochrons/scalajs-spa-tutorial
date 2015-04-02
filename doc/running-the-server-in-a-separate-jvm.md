# Running the server in a separate JVM

For fast development, it's nice to have the SBT console available even while the server is running. So instead of `spaJVM/run` you should use
the great [Revolver plugin](https://github.com/spray/sbt-revolver) and run the server with `re-start` and `re-stop`. This way you can start your
server and then instruct SBT to track changes to the client code with `~fastOptJS` and all your client changes are automatically deployed
without restarting your server.

To configure the Revolver you'll need the following

```scala
import spray.revolver.RevolverPlugin._
...
jvmSettings(Revolver.settings: _*).
...
// set some basic options when running the project with Revolver
javaOptions in Revolver.reStart ++= Seq("-Xmx1G"),
// configure a specific port for debugging, so you can easily debug multiple projects at the same time if necessary
Revolver.enableDebugging(port = 5111, suspend = false)
```

