package spatutorial.client.modules

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Icon._
import spatutorial.client.components._
import spatutorial.client.services._
import spatutorial.client.ukko.{EventType, ChangeEvent}

object MainMenu {

  case class MenuProps(activeLocation: MainRouter.Loc, router: MainRouter.Router)

  case class State(todoCount:Int)

  case class MenuItem(label: (State) => ReactNode, icon: Icon, location: MainRouter.Loc)

  class Backend(t: BackendScope[MenuProps, State]) extends OnUnmount {
    def mounted():Unit = {
      // listen to change events
      val removeListener = TodoStore.addListener(ChangeEvent, updated)
      // register things to do when unmounted
      onUnmount {
        removeListener()
      }
      MainDispatcher.dispatch(RefreshTodos)
    }

    def updated(event:EventType, store:TodoStore):Unit = {
      // count how many incomplete todos there are
      t.modState(_ => State(store.todos.count(!_.completed)) )
    }
  }

  // build the Todo menu item, showing the number of open todos
  private def buildTodoMenu(S:State):ReactNode = Seq(
    <.span("Todo "),
    if(S.todoCount > 0) <.span(^.className := "label label-danger label-as-badge", S.todoCount) else <.span()
  )

  private val menuItems = Seq(
    MenuItem(_ => "Dashboard", Icon.dashboard, MainRouter.dashboardLoc),
    MenuItem(buildTodoMenu, Icon.check, MainRouter.todoLoc)
  )

  private val MainMenu = ReactComponentB[MenuProps]("MainMenu")
    .initialState(new State(0))
    .backend(new Backend(_))
    .render((P, S, B) => {
    <.ul(^.className := "nav navbar-nav")(
      // build a list of menu items
      for (item <- menuItems) yield {
        <.li((P.activeLocation == item.location) ?= (^.className := "active"),
          P.router.link(item.location)(item.icon, " ", item.label(S))
        )
      }
    )
  })
    .componentDidMount(_.backend.mounted())
    .build

  def apply(props: MenuProps) = MainMenu(props)
}
