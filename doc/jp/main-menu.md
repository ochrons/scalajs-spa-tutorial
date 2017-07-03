# メインメニュー

メインメニューは、現在の場所とルーターをプロパティとして指定したよくあるReactコンポーネントです。 メニューの内容はクラスの中に静的に定義されます。なぜなら、参照される場所はコンパイル時にすべて知っているからです。 動的なシステムを使いたいメニューもあるでしょうが、このシステムでは静的な定義でうまくいきます。

```scala
case class Props(ctl: RouterCtl[Loc], currentLoc: Loc, proxy: ModelProxy[Option[Int]])

case class MenuItem(idx: Int, label: (Props) => VdomNode, icon: Icon, location: Loc)

// Todoメニュー項目を作成して、未完了のTodo数を表示します
private def buildTodoMenu(props: Props): VdomElement = {
  val todoCount = props.proxy().getOrElse(0)
  Seq(
    <.span("Todo "),
    <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount).when(todoCount > 0)
  )
}

private val menuItems = Seq(
  MenuItem(1, _ => "Dashboard", Icon.dashboard, DashboardLoc),
  MenuItem(2, buildTodoMenu, Icon.check, TodoLoc)
)
```

各メニュー項目に対して、ラベル、アイコン、および `routerConfig`に登録された場所を生成する関数を定義します。 ダッシュボード用
ラベルはシンプルなテキストですが、Todoでは `ModelProxy`プロパティを介して取得する未完了のTodo数もレンダリングします。

メニューをレンダリングするには、項目をループして適切なタグを作成するだけです。 リンクについては、プロパティで提供されるRouterCtlを使用する必要があります。

```scala
private class Backend(t: BackendScope[Props, _]) {
  def mounted(props: Props) = {
    // Todoをリフレッシュするメッセージをディスパッチする
    Callback.ifTrue(props.proxy.value.isEmpty, props.proxy.dispatch(RefreshTodos))
  }

  def render(props: Props) = {
    <.ul(bss.navbar)(
      // メニュー項目のリストを作成する
      for (item <- menuItems) yield {
        <.li(^.key := item.idx, (^.className := "active").when(props.currentLoc == item.location),
          props.router.link(item.location)(item.icon, " ", item.label(props))
        )
      }
    )
  }
}

private val component = ScalaComponent.builder[Props]("MainMenu")
  .renderBackend[Backend]
  .componentDidMount(scope => scope.backend.mounted(scope.props))
  .build
```

さて、HTMLページを定義し、メニューを生成し、アクティブなコンポーネント（Dashboard）をプレースホルダ内に持っていたら、次は何が起こるでしょうか？

