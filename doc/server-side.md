# Server side

The tutorial server is very simplistic and does not represent a typical Spray application, but it's enough to provide some basic support
for the client side. Routing logic on the server side is defined using Spray DSL

```scala
startServer("0.0.0.0", port = port) {
  get {
    pathSingleSlash {
      // serve the main page
      if(Config.productionMode)
        getFromResource("web/index-full.html")
      else
        getFromResource("web/index.html")
    } ~
      // serve other requests directly from the resource directory
      getFromResourceDirectory("web")
  } ~ post {
    path("api" / Segments) { s =>
      extract(_.request.entity.data) { requestData =>
        ctx =>
          // handle API requests via autowire
          val result = Router.route[Api](apiService)(
            autowire.Core.Request(s, Unpickle[Map[String, ByteBuffer]].fromBytes(requestData.toByteString.asByteBuffer))
          )
          result.map(responseData => ctx.complete(HttpEntity(HttpData(ByteString(responseData)))))
      }
    }
  }
}
```

The main HTML page and related resources are provided directly from the project resources directory (coming from the `shared` sub-project, actually).
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
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Unit = {
    // TODO, update database etc :)
    println(s"Todo item was updated: $item")
    if(todos.exists(_.id == item.id)) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
    } else {
      // add a new item
      todos :+= item.copy(id = UUID.randomUUID().toString)
    }
  }
}
```

