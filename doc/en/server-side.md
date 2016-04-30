# Server side

The tutorial server is very simplistic and does not represent a typical Play application, but it's enough to provide some basic support
for the client side. Routing logic on the server side is defined using a Play controller 

```scala
object Application extends Controller {
  val apiService = new ApiService()

  def index = Action {
    Ok(views.html.index("SPA tutorial"))
  }

  def autowireApi(path: String) = Action.async(parse.raw) {
    implicit request =>
      println(s"Request path: $path")
      // get the request body as ByteString
      val b = request.body.asBytes(parse.UNLIMITED).get

      // call Autowire route
      Router.route[Api](apiService)(
        autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(b.asByteBuffer))
      ).map(buffer => {
        val data = Array.ofDim[Byte](buffer.remaining())
        buffer.get(data)
        Ok(data)
      })
  }

  def logging = Action(parse.anyContent) {
    implicit request =>
      request.body.asJson.foreach { msg =>
        println(s"CLIENT - $msg")
      }
      Ok("")
  }
}
```

The main HTML page and related resources are provided by Play's template engine from the `twirl` directory.
The interesting part is handling `api` requests using Autowire router. Like on the client side, Autowire takes care of the complicated stuff
so you just need to plug it in and let it do its magic. Boopickle takes care of deserializing the request and serializing the response into binary. 

The `ApiService` is just a normal class and it doesn't need to concern itself with
serialization or URL request path mappings. It just implements the same `Api` as it used on the client side. Simple, eh!

```scala
class ApiService extends Api {
  var todos = Seq(
    TodoItem("1", "Wear shirt that says 'Life'. Hand out lemons on street corner.", TodoLow, false),
    TodoItem("2", "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, false),
    TodoItem("3", "Walk away slowly from an explosion without looking back.", TodoHigh, false),
    TodoItem("4", "Sneeze in front of the pope. Get blessed.", TodoNormal, true)
  )

  override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"

  override def getTodos(): Seq[TodoItem] = {
    // provide some fake Todos
    println(s"Sending ${todos.size} Todo items")
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Seq[TodoItem] = {
    // TODO, update database etc :)
    if(todos.exists(_.id == item.id)) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
      println(s"Todo item was updated: $item")
    } else {
      // add a new item
      val newItem = item.copy(id = UUID.randomUUID().toString)
      todos :+= newItem
      println(s"Todo item was added: $newItem")
    }
    todos
  }

  // delete a Todo
  override def deleteTodo(itemId: String): Seq[TodoItem] = {
    println(s"Deleting item with id = $itemId")
    todos = todos.filterNot(_.id == itemId)
    todos
  }
}
```

