# Server side

튜토리얼 서버는 매우 단순하며 일반적인 Play 애플리케이션을 대표하지는 않지만 클라이언트 측에 대한 기본적인 지원만으로 충분합니다. 서버 측의 라우팅 로직은 Play 컨트롤러를 사용하여 정의됩니다.

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

메인 HTML 페이지와 관련 리소스는`twirl` 디렉토리에서 Play의 템플릿 엔진에 의해 제공됩니다.
흥미로운 부분은 Autowire 라우터를 사용하여`api` 요청을 처리하는 것입니다. 클라이언트 측에서와 마찬가지로, Autowire는 복잡한 내용을 처리하므로 플러그를 꽂아야 만 마술을 할 수 있습니다. Boopickle은 요청을 비 직렬화하고 응답을 2 진으로 직렬화합니다.

`ApiService`는 일반적인 클래스 일 뿐이며 직렬화 또는 URL 요청 경로 매핑에 신경 쓰지 않아도됩니다. 그것은 단지 클라이언트 측에서 사용 된 것과 동일한 'Api'를 구현합니다. 간단합니다.

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

