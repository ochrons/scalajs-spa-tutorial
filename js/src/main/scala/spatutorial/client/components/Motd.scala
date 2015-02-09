package spatutorial.client.components

import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import japgolly.scalajs.react.vdom.all._
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import spatutorial.client.components.Bootstrap._
import spatutorial.client.services.AjaxClient
import spatutorial.shared.Api
import autowire._

/**
 * This is a simple component demonstrating how to interact with the server
 */
object Motd {

  case class State(message: String)

  class Backend(t: BackendScope[Unit, State]) {
    def refresh() {
      // load a new message from the server
      AjaxClient[Api].motd("User X").call().foreach { message =>
        t.modState(_ => State(message))
      }
    }
  }

  // create the React component for holding the Message of the Day
  val Motd = ReactComponentB[Unit]("Motd")
    .initialState(State("loading...")) // show a loading text while message is being fetched from the server
    .backend(new Backend(_))
    .render((_, S, B) => {
    Panel(PanelProps("Message of the day"), div(S.message),
      Button(ButtonProps(B.refresh, CommonStyle.danger), Icon.refresh, span(" Update"))
    )
  })
    .componentDidMount(scope => {
    scope.backend.refresh()
  })
    .buildU

  def apply() = Motd()
}
