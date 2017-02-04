# Autowire and BooPickle

웹 클라이언트는 가장 일반적으로 Ajax *라는 서버와 통신합니다.이 기술은 매우 느슨하게 정의 된 기술 모음입니다. JQuery와 같은 가장 눈에 띄는 JavaScript 라이브러리는 브라우저에 노출 된 낮은 수준의 프로토콜에 대한 상위 수준의 액세스를 제공합니다. Scala.js는`dom.extensions.Ajax` (또는 scalajs-dom 0.8+의`dom.ext.Ajax`)에 멋진 Ajax 래퍼를 제공하지만 객체를 직렬화 / 비 직렬화하고 모든 더러운 세부적인 것을 처리하는 것은 여전히 지루합니다.

그러나 두려움이 없다면, 우리 친구 [Li Haoyi (lihaoyi)](https://github.com/lihaoyi)가 [Autowire](https://github.com/lihaoyi/autowire)라고 불리는 훌륭한 도서관을 만들고 출판 한 이래로 모든 것을 스스로 할 필요는 없습니다. 내 자신의 [BooPickle](https://github.com/ochrons/boopickle) 라이브러리와 결합하면 클라이언트 - 서버 통신을 쉽게 처리 할 수 있습니다. BooPickle은 바이너리 직렬화 형식을 사용하므로 JSON 형식을 선호한다면 [uPickle](https://github.com/lihaoyi/upickle)을 사용해보십시오. SPA 튜토리얼은 uPickle을 사용하여 직렬화에 사용되었으므로 저장소 기록을 탐색하여 관련 코드 [여기](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/js/src/main/scala/spatutorial/client/services/AjaxClient.scala)와 [여기](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/jvm/src/main/scala/spatutorial/server/MainApp.scala)를 볼 수 있습니다.

자신 만의 클라이언트 - 서버 통신 경로를 구축하려면 클라이언트 측에서 하나의 객체를 정의하고 서버 측에서 다른 하나의 객체를 정의해야합니다.

```scala
import boopickle.Default._

// client side
object AjaxClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
  override def doCall(req: Request): Future[ByteBuffer] = {
    dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }

  override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
  override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}
```

응용 프로그램과 관련된 유일한 변수는 서버 호출에 사용하려는 URL입니다. 그렇지 않으면 매크로의 마법을 통해 자동으로 생성 된 다른 모든 것. 서버 측은 심지어 더 간단합니다. 단지 Autowire가 직렬화에 BooPickle을 사용하고자 함을 알립니다.

```scala
import boopickle.Default._

// server side
object Router extends autowire.Server[ByteBuffer, Pickler, Pickler] {
  override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
  override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
}
```

이제`AjaxClient`가 설정되었으므로 서버를 호출하는 것은 아주 간단합니다.

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._

AjaxClient[Api].getTodos().call().foreach { todos =>
  println(s"Got some things to do $todos")
}
```

Autowire / BooPickle 마법에 액세스하고 미래에 대한 실행 컨텍스트를 제공하려면이 세 가지 가져 오기가 필요합니다.

`Api`는 클라이언트와 서버가 공유하는 단순한 특성입니다.

```scala
trait Api {
  // message of the day
  def motd(name:String) : String

  // get Todo items
  def getTodos() : Seq[TodoItem]

  // update a Todo
  def updateTodo(item: TodoItem): Seq[TodoItem]

  // delete a Todo
  def deleteTodo(itemId: String): Seq[TodoItem]
}
```

직렬화가 가능하고 직렬화 할 수없는 것에 대해서는 [BooPickle documentation](https://github.com/ochrons/boopickle)을 확인하십시오. 데이터가 복잡하다면 다른 것을 사용해야 할 수도 있습니다. 사례 클래스, 기본 모음 및 기본 데이터 형식은 안전한 방법입니다.

그렇다면 이것이 서버 측에서 어떻게 작동합니까?
