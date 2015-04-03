# Autowire and uPickle

Web clients communicate with the server most commonly with *Ajax* which is quite loosely defined collection of techniques. Most notable
JavaScript libraries like JQuery provide higher level access to the low level protocols exposed by the browser. Scala.js provides a nice
Ajax wrapper in `dom.extensions.Ajax` (or `dom.ext.Ajax` in scalajs-dom 0.8+) but it's still quite tedious to serialize/deserialize objects
and take care of all the dirty little details.

But fear not, there is no need to do all that yourself as our friend [Li Haoyi (lihaoyi)](https://github.com/lihaoyi) has created and
published two great libraries called [uPickle](https://github.com/lihaoyi/upickle) and [Autowire](https://github.com/lihaoyi/autowire).

To build your own client-server communication pathway all you need to do is to define a single object on the client side and another on the
server side.

```scala
// client side
object AjaxClient extends autowire.Client[String, upickle.Reader, upickle.Writer]{
  override def doCall(req: Request): Future[String] = {
    dom.extensions.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = upickle.write(req.args),
      headers = Map("Content-Type" -> "application/json;charset=UTF-8")
    ).map(_.responseText)
  }

  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
```

The only variable specific to your application is the URL you want to use to call the server. Otherwise everything else it automatically
generated for you through the magic of macros. The server side is even simpler, just letting Autowire know that you want to use uPickle
for serialization.

```scala
// server side
object Router extends autowire.Server[String, upickle.Reader, upickle.Writer]{
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
```

Now that you have the `AjaxClient` set up, calling server is as simple as

```scala
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._

AjaxClient[Api].getTodos().call().foreach { todos =>
  println(s"Got some things to do $todos")
}
```

Note that you need those two imports to access the Autowire magic and to provide an execution context for the futures.

The `Api` is just a simple trait shared between the client and server.

```scala
trait Api {
  // message of the day
  def motd(name:String) : String

  // get Todo items
  def getTodos() : Seq[TodoItem]

  // update a Todo
  def updateTodo(item:TodoItem)
}
```

Please check out uPickle documentation on what it can and cannot serialize. You might need to use something else if your data is complicated.
Case classes, base collections and basic data types are a safe bet.

So how does this work on the server side?

