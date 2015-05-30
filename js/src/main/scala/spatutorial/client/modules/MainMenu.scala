package spatutorial.client.modules

import japgolly.scalajs.react.extra.router2.RouterCtl
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

  case class Props(ctl: RouterCtl[Loc], currentLoc:Loc, todos: Rx[Seq[TodoItem]])

  case class MenuItem(label: (Props) => ReactNode, icon: Icon, location: Loc)

  class Backend(t: BackendScope[Props, _]) extends OnUnmount {
    def mounted(): Unit = {
      // hook up to Todo changes
      val obsItems = t.props.todos.foreach { _ => t.forceUpdate() }
      onUnmount {
        // stop observing when unmounted (= never in this SPA)
        obsItems.kill()
      }
      MainDispatcher.dispatch(RefreshTodos)
    }
  }

  // build the Todo menu item, showing the number of open todos
  private def buildTodoMenu(props: Props): ReactNode = {
    val todoCount = props.todos().count(!_.completed)
    Seq(
      <.span("Todo "),
      if (todoCount > 0) <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount) else <.span()
    )
  }

  private val menuItems = Seq(
    MenuItem(_ => "Dashboard", Icon.dashboard, DashboardLoc),
    MenuItem(buildTodoMenu, Icon.check, TodoLoc)
  )

  private val MainMenu = ReactComponentB[Props]("MainMenu")
    .stateless
    .backend(new Backend(_))
    .render((P, _, B) => {
    <.ul(bss.navbar)(
      // build a list of menu items
      for (item <- menuItems) yield {
        <.li((P.currentLoc == item.location) ?= (^.className := "active"),
          P.ctl.link(item.location)(item.icon, " ", item.label(P))
        )
      }
    )
  })
    .componentDidMount(_.backend.mounted())
    .build

  def apply(props: Props) = MainMenu(props)
}
