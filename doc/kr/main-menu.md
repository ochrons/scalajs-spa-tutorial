# Main menu

주 메뉴는 현재 위치와 라우터가 속성으로 주어지는 또 다른 React 구성 요소입니다. 메뉴의 내용은 클래스 자체 내에서 정적으로 정의됩니다. 왜냐하면 참조 된 위치는 모두 컴파일 타임에 알려지기 때문입니다. 동적 인 시스템에서 사용하고 싶은 다른 종류의 메뉴가 있지만, 여기에서는 정적 인 것으로 좋습니다.

```scala
case class Props(ctl: RouterCtl[Loc], currentLoc: Loc, proxy: ModelProxy[Option[Int]])

case class MenuItem(idx: Int, label: (Props) => VdomNode, icon: Icon, location: Loc)

// build the Todo menu item, showing the number of open todos
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

각 메뉴 항목에 대해 레이블, 아이콘 및 `routerConfig`에 등록 된 위치를 생성하는 함수를 정의합니다. 대시 보드의 경우 레이블은 단순한 텍스트이지만 Todo의 경우 `ModelProxy` 속성을 통해 열린 열린 번호의 수를 렌더링합니다.

메뉴를 렌더링하려면 항목을 반복하고 적절한 태그를 만듭니다. 링크의 경우 속성에 제공된 `RouterCtl`을 사용해야합니다.

```scala
private class Backend(t: BackendScope[Props, _]) {
  def mounted(props: Props) = {
    // dispatch a message to refresh the todos
    Callback.ifTrue(props.proxy.value.isEmpty, props.proxy.dispatchCB(RefreshTodos))
  }

  def render(props: Props) = {
    <.ul(bss.navbar)(
      // build a list of menu items
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

이제 HTML 페이지를 정의하고 메뉴를 생성하고 자리 표시 자 내에 활성 구성 요소 (대시 보드)를 만들었습니까? 다음에는 어떻게됩니까?

