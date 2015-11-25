package spatutorial.client.modules

import diode.react.ComponentModel
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{DashboardLoc, Loc, TodoLoc}
import spatutorial.client.components.Bootstrap.CommonStyle
import spatutorial.client.components.Icon._
import spatutorial.client.components._
import spatutorial.client.services._

import scalacss.ScalaCssReact._

object MainMenu {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(ctl: RouterCtl[Loc], currentLoc: Loc, cm: ComponentModel[Option[Todos]])

  case class MenuItem(idx: Int, label: (Props) => ReactNode, icon: Icon, location: Loc)

  class Backend(t: BackendScope[Props, _]) {
    def mounted(props: Props) = Callback {
      // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
      if(props.cm.value.isEmpty)
        props.cm.dispatch(RefreshTodos)
    }
  }

  // build the Todo menu item, showing the number of open todos
  private def buildTodoMenu(props: Props): ReactElement = {
    val todoCount = props.cm().map(_.items.count(!_.completed)).getOrElse(0)
    <.span(
      <.span("Todo "),
      if (todoCount > 0) <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount) else <.span()
    )
  }

  private val menuItems = Seq(
    MenuItem(1, _ => "Dashboard", Icon.dashboard, DashboardLoc),
    MenuItem(2, buildTodoMenu, Icon.check, TodoLoc)
  )

  private val MainMenu = ReactComponentB[Props]("MainMenu")
    .stateless
    .backend(new Backend(_))
    .render_P((P) => {
    <.ul(bss.navbar)(
      // build a list of menu items
      for (item <- menuItems) yield {
        <.li(^.key := item.idx, (P.currentLoc == item.location) ?= (^.className := "active"),
          P.ctl.link(item.location)(item.icon, " ", item.label(P))
        )
      }
    )
  })
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(ctl: RouterCtl[Loc], currentLoc: Loc, cm: ComponentModel[Option[Todos]]) = MainMenu(Props(ctl, currentLoc, cm))
}
