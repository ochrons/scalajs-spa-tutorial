# AutowireとBooPickle

Webクライアントとサーバ間での通信では、*Ajax*が広く用いられています。*Ajax*は非常にゆるく定義された技術の集合体です。

JQueryのように非常に有名なJavaScriptライブラリは、高レベルのアクセス手段から、ブラウザが開示している低レベルのプロトコルへのアクセス手段までを提供しています。
Scala.jsは、 `dom.extensions.Ajax` (もしくは、scalajs-dom 0.8+においては、`dom.ext.Ajax`) において、良いAjaxラッパーを提供しています。今のところ、このラッパーは非常に冗長(tedious)なシリアライズ/デシリアライズ オブジェクトの集まりであり、いくつものつまらない些細な事柄(all the dirty little details)に注意しなければなりません。

しかし、こうした委細の全てに一人で立ち向かう必要はありません。
我らが[Li Haoyi (lihaoyi)](https://github.com/lihaoyi)が、  [Autowire](https://github.com/lihaoyi/autowire)という素晴らしいライブラリを作成し、公開してくれています。
私が作った、[BooPickle](https://github.com/ochrons/boopickle)ライブラリと組み合わせると、クライアント-サーバ間の通信を容易に行うことができます。

ただし、BooPickleではバイナリでのシリアライズ化フォーマットを用いられます。JSONフォーマットを使いたい場合には、[uPickle](https://github.com/lihaoyi/upickle)を選択肢としてみてください。
SPAチュートリアルにおいては、当初、シリアライズにuPickleをを用いていたため、リポジトリのヒストリの[ここ](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/js/src/main/scala/spatutorial/client/services/AjaxClient.scala)
と [ここ](https://github.com/ochrons/scalajs-spa-tutorial/blob/628bf9308aaebe7f3d0527007ef604801988ef42/jvm/src/main/scala/spatutorial/server/MainApp.scala)において、uPickleによる参照コードを見ることができます。

クライアント-サーバ間通信の経路をビルドするにあたっては、クライアント側及びサーバ側にそれぞれ単一のオブジェクトを定義することだけが必要とされます。

```scala
import boopickle.Default._

// クライアント側
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
アプリケーションにおいて指定する必要のある変数は、用いたいサーバのURLのみです。他のすべては、マクロの魔法を通じ、自動的に生成されます。

サーバ側はさらにシンプルで、単にAutowireにBooPickleをシリアライズに用いることを知らせるだけです。

```scala
import boopickle.Default._

// サーバ側
object Router extends autowire.Server[ByteBuffer, Pickler, Pickler] {
  override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
  override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
}
```
これで、`AjaxClient`のセットアップは終了です。 サーバの呼び出し方は以下のようにシンプルです。

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._

AjaxClient[Api].getTodos().call().foreach { todos =>
  println(s"Got some things to do $todos")
}
```

Autowire/BooPickleの魔法を用いて以後の実行環境を整えるためには、上3つのimport文が必要であることに留意してください。


`Api` は、クライアント-サーバ間で共有されるシンプルなtraitです。

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

BooPickleがシリアライズできるもの、シリアライズできないものは、[BooPickleドキュメント](https://github.com/ochrons/boopickle)で知ることができます。
もし、用いるデータが複雑な場合、他のライブラリを用いる必要があるかもしれません。ケースクラス、基本的なコレクション、そしてベーシックなデータ型を用いるのが安全です。

さて、サーバ側ではどのような動作がなされるのでしょうか？
