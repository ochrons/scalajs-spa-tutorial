package spatutorial.client.components

import diode.react.ReactPot._
import diode.react._
import diode.util.Pot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Bootstrap._
import spatutorial.client.services.UpdateMotd

/**
  * This is a simple component demonstrating how to display async data coming from the server
  */
object Motd {

  // create the React component for holding the Message of the Day
  val Motd = ReactComponentB[ComponentModel[Pot[String]]]("Motd")
    .render_P { cm =>
      Panel(Panel.Props("Message of the day"),
        // render messages depending on the state of the Pot
        cm().renderPending(_ > 500, _ => <.p("Loading...")),
        cm().renderFailed(ex => <.p("Failed to load")),
        cm().render(m => <.p(m)),
        Button(Button.Props(cm.dispatch(UpdateMotd()), CommonStyle.danger), Icon.refresh, " Update")
      )
    }
    .componentDidMount(scope =>
      // update only if Motd is empty
      Callback.ifTrue(scope.props.value.isEmpty, scope.props.dispatch(UpdateMotd()))
    )
    .build

  def apply(cm: ComponentModel[Pot[String]]) = Motd(cm)
}
