package spatutorial.client.modules

import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import spatutorial.client.services.TodoStore

object AppLinks {
}

object MainRouter extends RoutingRules {
  // build a baseUrl, this method works for both local and server addresses (assuming you use #)
  val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))

  // register the modules and store locations
  val dashboardLoc = register(rootLocation(Dashboard.component))
  val todoLoc = register(location("#todo", Todo(TodoStore)))

  // functions to provide links (<a href...>) to routes
  def dashboardLink = router.link(dashboardLoc)
  def todoLink = router.link(todoLoc)
  def routerLink(loc: Loc) = router.link(loc)

  // initialize router and its React component
  val router = routingEngine(baseUrl)
  val routerComponent = Router.component(router)

  // redirect all invalid routes to dashboard
  override protected val notFound = redirect(dashboardLoc, Redirect.Replace)

  /**
   * Creates the basic page structure under the body tag.
   *
   * @param ic
   * @return
   */
  override protected def interceptRender(ic: InterceptionR) = {
    <.div(
      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
        <.div(^.className := "container")(
          <.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
          <.div(^.className := "collapse navbar-collapse")(
            MainMenu(MainMenu.Props(ic.loc, TodoStore.todos))
          )
        )
      ),
      // currently active module is shown in this container
      <.div(^.className := "container")(ic.element)
    )
  }
}
