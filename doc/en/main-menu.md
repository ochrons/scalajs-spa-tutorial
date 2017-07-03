# Main menu

The main menu is just another React component that is given the current location and the router as properties. The contents of the menu is defined
statically within the class itself, because the referred locations are anyway all known at compile time. For other kinds of menus you'd want to use
a dynamic system, but static is just fine here.

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

For each menu item we define a function to generate the label, an icon and the location that was registered in the `routerConfig`. For Dashboard
the label is simple text, but for Todo we also render the number of open todos, which we get through the `ModelProxy` property.

To render the menu we just loop over the items and create appropriate tags. For links we need to use the `RouterCtl` provided in the properties.

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

Ok, we've got the HTML page defined, menu generated and the active component (Dashboard) within the placeholder, what happens next?

