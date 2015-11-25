package spatutorial.client.modules

import japgolly.scalajs.react.extra.router.RouterCtl
import spatutorial.client.SPAMain.{TodoLoc, DashboardLoc, Loc}

import scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._
import rx._
import rx.ops._
import spatutorial.client.components.Bootstrap.CommonStyle
import spatutorial.client.components.Icon._
import spatutorial.client.components._
import spatutorial.client.services._
import spatutorial.shared.TodoItem

object MainMenu {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class Props(ctl: RouterCtl[Loc], currentLoc: Loc, todos: Rx[Seq[TodoItem]])

  case class MenuItem(idx: Int, label: (Props) => ReactNode, icon: Icon, location: Loc)

  class Backend(t: BackendScope[Props, _]) extends OnUnmount {
    def mounted(props: Props) = Callback {
      // hook up to Todo changes
      val obsItems = props.todos.foreach { _ => t.forceUpdate.runNow() }
      onUnmount(Callback {
        // stop observing when unmounted (= never in this SPA)
        obsItems.kill()
      })
      MainDispatcher.dispatch(RefreshTodos)
    }
  }

  // build the Todo menu item, showing the number of open todos
  private def buildTodoMenu(props: Props): ReactElement = {
    val todoCount = props.todos().count(!_.completed)
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

  def apply(props: Props) = MainMenu(props)
}
