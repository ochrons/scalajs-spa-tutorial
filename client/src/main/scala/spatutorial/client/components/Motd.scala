package spatutorial.client.components

import autowire._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{Callback, BackendScope, ReactComponentB}
import spatutorial.client.components.Bootstrap._
import spatutorial.client.services.AjaxClient
import spatutorial.shared.Api
import boopickle.Default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * This is a simple component demonstrating how to interact with the server
 */
object Motd {

  case class State(message: String)

  class Backend(t: BackendScope[Unit, State]) {
    def refresh() = Callback {
      // load a new message from the server
      AjaxClient[Api].welcome("User X").call().foreach { message =>
        t.modState(_ => State(message)).runNow()
      }
    }

    def render(S: State) = {
      Panel(Panel.Props("Message of the day"), <.div(S.message),
        Button(Button.Props(refresh(), CommonStyle.danger), Icon.refresh, " Update")
      )
    }
  }

  // create the React component for holding the Message of the Day
  val Motd = ReactComponentB[Unit]("Motd")
    .initialState(State("loading...")) // show a loading text while message is being fetched from the server
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.refresh())
    .buildU

  def apply() = Motd()
}
