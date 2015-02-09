package spatutorial.client.modules

import japgolly.scalajs.react.extra.router.{ApprovedPath, Location, Redirect, RoutingRules}
import japgolly.scalajs.react.vdom.all._
import spatutorial.client.components.Icon._

case class RouterMenuItem(label: String, icon: Icon, location: Location[_])

/**
 * Provides a base for all routes. Allows registration of modules to be displayed in the main menu
 */
trait BaseRoute extends RoutingRules {
  var menuItems = Vector.empty[RouterMenuItem]

  def registerMenu(item: RouterMenuItem) = menuItems :+= item
}

// ordering of routes here determines the order in the main menu as well
trait AllRoutes extends Dashboard.DashboardRoute with TODO.TODORoute

object MainRouter extends AllRoutes {

  // redirect all invalid routes to dashboard
  override protected val notFound = redirect(dashboard, Redirect.Replace)

  /**
   * Creates the basic page structure under the body tag.
   *
   * @param ic
   * @return
   */
  override protected def interceptRender(ic: InterceptionR) = {
    div(
      nav(cls := "navbar navbar-inverse navbar-fixed-top")(
        div(cls := "container")(
          div(cls := "navbar-header")(span(cls := "navbar-brand")("SPA Tutorial")),
          div(cls := "collapse navbar-collapse")(
            ul(cls := "nav navbar-nav")(
              // build a list of registered menu items
              for (item <- menuItems) yield {
                li((ic.loc == item.location) ?= (cls := "active"),
                  ic.router.link(item.location.asInstanceOf[ApprovedPath[P]])(item.icon, " ", item.label))
              }
            )
          )
        )
      ),
      // currently active module is shown in this container
      div(cls := "container")(ic.element)
    )
  }
}
