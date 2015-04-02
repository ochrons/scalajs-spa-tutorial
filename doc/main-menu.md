# Main menu

The main menu is just another React component that is given the current location and the router as properties. The contents of the menu is defined
statically within the class itself, because the referred locations are anyway all known at compile time. For other kinds of menus you'd want to use
a dynamic system, but static is just fine here.

```scala
case class MenuItem(label: (Props) => ReactNode, icon: Icon, location: MainRouter.Loc)

private val menuItems = Seq(
  MenuItem(_ => "Dashboard", Icon.dashboard, MainRouter.dashboardLoc),
  MenuItem(buildTodoMenu, Icon.check, MainRouter.todoLoc)
)

private def buildTodoMenu(props: MenuProps): ReactNode = {
  val todoCount = props.todos().count(!_.completed)
  Seq(
    <.span("Todo "),
    if (todoCount > 0) <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount) else <.span()
  )
}
```

For each menu item we define a function to generate the label, an icon and the location that was registered in the `MainRouter`. For Dashboard
the label is simple text, but for Todo we also render the number of open todos.

To render the menu we just loop over the items and create appropriate tags. For links we need to use the `router` provided in the properties.

```scala
val MainMenu = ReactComponentB[MenuProps]("MainMenu")
  .render(P => {
  <.ul(bss.navbar)(
    // build a list of menu items
    for (item <- menuItems) yield {
      <.li((P.activeLocation == item.location) ?= (^.className := "active"),
        MainRouter.routerLink(item.location)(item.icon, " ", item.label(P))
      )
    }
  )
})
  .build
```

Ok, we've got the HTML page defined, menu generated and the active component (Dashboard) within the placeholder, what happens next?

