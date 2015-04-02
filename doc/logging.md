# Logging

Most of us are used to great logging infrastructure available on the server side and sometimes it helps in situations where direct debugging is difficult
or plain impossible (for example a customer is using your app). Luckily there are nice logging libraries available also for Javascript that we can utilize
through thin facades.

To emulate log4j style API, we'll define a `LoggerFactory` providing `Logger` instances. These will hook up to the underlying
[Javascript library](http://log4javascript.org/) to provide the real functionality. See
[`Log4JavaScript.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/logger/Log4JavaScript.scala) for details.

The `package object logger` provides a default logger called `log` for easy access to logging functionality. If you want to create separate loggers,
you may do so through `LoggerFactory.getLogger(name)` method. To create an entry in the log, all you need to do is call the appropriate log-level function
with a message and an optional `Exception`.

```scala
log.debug(s"User selected ${items.size} items")

log.error("Invalid response from server", ex)
```

The default logger prints all messages to the browser console, but you can also use a more advanced popup window logger, to analyze log messages with
better granularity and filtering. Use `getPopupLogger` to create such logger.

## Sending client logs to the server

The logging library also provides functionality to send all your log messages to the server, for easier analysis in error situations. It packages each
log message into a small JSON object and POSTs it to the specified URL. On the server side we define a path to receive and print those log messages.

```scala
path("logging") {
  entity(as[String]) { msg =>
    ctx =>
      println(s"ClientLog: $msg")
    ctx.complete(StatusCodes.OK)
  }
}
```

To enable server side logging, call `log.enableServerLogging("/logging")`. In the server logs the client log messages will be shown as below:

```
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500652089,"level":"INFO","url":"http://localhost:8080/#todo","message":"This message goes to server as well"}]
sharedProjectJVM Sending 4 Todo items
sharedProjectJVM Sending 4 Todo items
sharedProjectJVM Todo item was updated: TodoItem(3,Walk away slowly from an explosion without looking back.,TodoHigh,false)
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500661456,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500664865,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"Todo editing cancelled"}]
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500668485,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500671017,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500671751,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM ClientLog: [{"logger":"Log","timestamp":1425500672101,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"Todo edited:
  TodoItem(3,Walk away slowly from an explosion without looking back.,TodoNormal,false)"}]
```

Many of the advanced features of the underlying logging library are not exposed in this tutorial project, but you can look at the current implementation and
continue from there if you find the missing features useful!

## Limiting log messages in production

While extensive debug messages are a life saver in development, you don't want to flood your customer's browser console with debug messages. To disable
low level log messages in the production build we'll use a special Scala annotation called [@elidable](http://www.scala-lang.org/api/current/index.html#scala.annotation.elidable).
It works much like `#ifdef` in C/C++ removing the function and all calls to the function in the final byte-code. Therefore there is no performance penalty
whatsoever even if you litter your code with hundreds of `log.debug` calls, as they are all totally optimized away.

```scala
  @elidable(FINEST) def trace(msg: String, e: Exception): Unit
  @elidable(FINEST) def trace(msg: String): Unit
  @elidable(FINE) def debug(msg: String, e: Exception): Unit
  @elidable(FINE) def debug(msg: String): Unit
  @elidable(INFO) def info(msg: String, e: Exception): Unit
  @elidable(INFO) def info(msg: String): Unit
```

To control what calls to eliminate and what to keep, use a `scalac` command line option `-Xelide-below <level>`. This is automatically set to `WARNING` in
the release command.
```scala
// in settings we have scalacOptions ++= elideOptions.value,
set elideOptions in js := Seq("-Xelide-below", "WARNING")
```

In the production build all log calls below the `WARNING` level will be optimized away.

