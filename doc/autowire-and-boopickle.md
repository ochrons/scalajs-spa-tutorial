# Autowire and BooPickle

Web clients communicate with the server most commonly with *Ajax* which is quite loosely defined collection of techniques. Most notable
JavaScript libraries like JQuery provide higher level access to the low level protocols exposed by the browser. Scala.js provides a nice
Ajax wrapper in `dom.extensions.Ajax` (or `dom.ext.Ajax` in scalajs-dom 0.8+) but it's still quite tedious to serialize/deserialize objects
and take care of all the dirty little details.

But fear not, there is no need to do all that yourself as our friend [Li Haoyi (lihaoyi)](https://github.com/lihaoyi) has created and
published a great library called [Autowire](https://github.com/lihaoyi/autowire). Combined with the author's very own 
[BooPickle](https://github.com/ochrons/boopickle) library you can easily handle client-server communication. Note that BooPickle uses
binary serialization format, so if you'd prefer a JSON format, consider using [uPickle](https://github.com/lihaoyi/upickle). As SPA tutorial
used to use uPickle for serialization, you can browse the repository history to see the relevant code 
[here](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/js/src/main/scala/spatutorial/client/services/AjaxClient.scala)
and [here](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/jvm/src/main/scala/spatutorial/server/MainApp.scala).

To build your own client-server communication pathway all you need to do is to define a single object on the client side and another on the
server side.

```scala
import boopickle._

// client side
object AjaxClient extends autowire.Client[ByteBuffer, Unpickler, Pickler] {
  override def doCall(req: Request): Future[ByteBuffer] = {
    post(url = "/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }
  
  def read[Result: Unpickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}
```

The only variable specific to your application is the URL you want to use to call the server. Otherwise everything else it automatically
generated for you through the magic of macros. The server side is even simpler, just letting Autowire know that you want to use BooPickle
for serialization.

```scala
import boopickle._

// server side
object Router extends autowire.Server[ByteBuffer, Unpickler, Pickler] {
  def read[Result: Unpickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
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

Please check out [BooPickle documentation](https://github.com/ochrons/boopickle) on what it can and cannot serialize. You might need to use 
something else if your data is complicated. Case classes, base collections and basic data types are a safe bet.

So how does this work on the server side?
