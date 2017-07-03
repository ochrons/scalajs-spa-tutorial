# ダッシュボード

[ダッシュボードモジュール](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/modules/Dashboard.scala)は本当にシンプルです。
それには内部状態もバックエンド機能も含まれていないため、Reactコンポーネントの面では問題ありません。このモジュールは基本的に`Motd`と`Chart`という2つのコンポーネントのプレースホルダです。
唯一のメソッドは、コンポーネントがReactによってマウントされたときにコンポーネントをレンダリングする`render`メソッドです。また、このコンポーネントは物事をシンプルに保つためにChartコンポーネントの偽データを提供します。

```scala
// ダッシュボード用Reactコンポーネントの作成
private val component = ScalaComponent.builder[Props]("Dashboard")
  .render_P { case Props(router, proxy) =>
    <.div(
      // header、MessageOfTheDay、chartコンポーネント
      <.h2("Dashboard"),
      // ModelProxyの接続を使用して、Motdにモデルの部分的なビューのみを与える
      proxy.connect(_.motd)(Motd(_)),
      Chart(cp),
      // ToDoビューへのリンクを作成する
      <.div(router.link(TodoLoc)("Check your todos!"))
    )
  }.build
```

## 今日のメッセージ

[Motdコンポーネント](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/Motd.scala)は*今日のメッセージ*をサーバーから取得し、パネル上に表示するシンプルなReactコンポーネントです。
`Motd`はプロパティでメッセージを与えられます（`Pot`と`ModelProxy`）。
```scala
val Motd = ScalaComponent.builder[ModelProxy[Pot[String]]]("Motd")
  .render_P { proxy =>
    Panel(Panel.Props("Message of the day"),
      // Potの状態に応じてメッセージを表示する
      proxy().renderPending(_ > 500, _ => <.p("Loading...")),
      proxy().renderFailed(ex => <.p("Failed to load")),
      proxy().render(m => <.p(m)),
      Button(Button.Props(proxy.dispatch(UpdateMotd()), CommonStyle.danger), Icon.refresh, " Update")
    )
  }
  .componentDidMount(scope =>
    // Motdが空の場合のみ更新する
    Callback.ifTrue(scope.props.value.isEmpty, scope.props.dispatch(UpdateMotd()))
  )
  .build
```
Reactコンポーネントは、一連の関数呼び出しによって定義されます。これらの呼び出しのそれぞれは、コンポーネントの型を変更します。つまり、最初に`render`を行わないと、`componentDidMount`を呼び出すことはできません

もちろん、実際にメッセージを取得するには、サーバーにリクエストを送る必要があります。コンポーネントがマウントされたときにこれを自動的に行うには、`componentDidMount`メソッド内で`dispatch`にフックします。ただし、すでにメッセージに値がない場合に限ります。

`ModelProxy`と`Pot`の使用については[後で](todo-module-and-data-flow.md)詳しく説明します.

## 他のルートへのリンク

ルートの背後にある別のモジュールにユーザーを誘導するリンクを作成する必要があることがあります。これらのリンクを型安全に作成するために、
チュートリアルでは`RouterCtl`のインスタンスをコンポーネントに渡します。

```scala
ctl.link(TodoLoc)("Check your todos!")
```

