package spatutorial.client

import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router2._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import spatutorial.client.components.GlobalStyles
import spatutorial.client.logger._
import spatutorial.client.modules._
import spatutorial.client.services.TodoStore

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._

@JSExport("SPAMain")
object SPAMain extends js.JSApp {

  // Define the locations (pages) used in this application
  sealed trait Loc

  case object DashboardLoc extends Loc

  case object TodoLoc extends Loc

  // configure the router
  val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
    import dsl._

    (staticRoute(root, DashboardLoc) ~> renderR(ctl => Dashboard.component(ctl))
      | staticRoute("#todo", TodoLoc) ~> renderR(ctl => Todo(TodoStore)(ctl))
      ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
  }.renderWith(layout)

  // base layout for all pages
  def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
    <.div(
      // here we use plain Bootstrap class names as these are specific to the top level layout defined here
      <.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
        <.div(^.className := "container")(
          <.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
          <.div(^.className := "collapse navbar-collapse")(
            MainMenu(MainMenu.Props(c, r.page, TodoStore.todos))
          )
        )
      ),
      // currently active module is shown in this container
      <.div(^.className := "container")(r.render())
    )
  }

  @JSExport
  def main(): Unit = {
    log.warn("Application starting")
    // send log messages also to the server
    log.enableServerLogging("/logging")
    log.info("This message goes to server as well")

    // create stylesheet
    GlobalStyles.addToDocument()
    // create the router
    val router = Router(BaseUrl(dom.window.location.href.takeWhile(_ != '#')), routerConfig)
    // tell React to render the router in the document body
    React.render(router(), dom.document.getElementById("root"))
  }
}
