# サーバ側

チュートリアル用のサーバは非常にシンプルであり、典型的なPlayアプリケーションではありませんが、クライアント側のコードの基本的なサポートには十分です。
サーバ側のルーティングロビックはPlayのコントローラーを用いて定義されます。

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
        autowire.Core.Request(path.split("/"), Unpicクライアント側と同様に、kle[Map[がtring, ByteBuffer]].fromBytes(b.asByteBuffer))そのため、単に、
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

mainのHTMLページ及び関連するリソースは、`twirl`ディレクトリからPlayのテンプレートエンジンにより提供されます。
興味深いのは、Autowireルーターを用いての`api`リクエストの扱い方です。
クライアント側と同様に、Autowireが、複雑な事柄を取り扱います。すなわち、単にAutowireをプラグするだけで、そのマジックは発揮されます。
Boopickleが、リクエストのデシリアライズと、レスポンスのバイナリへのシリアライズを行います。
`ApiService`は、単なる通常のクラスであり、それ自体はシリアライズやURlリクエストのマッピングに関与しません。
このサービスは、クライアント側で用いられたのと同一の`Api` を実装します。
シンプルだね！

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
