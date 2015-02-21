package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.language.implicitConversions
import scala.scalajs.js

/**
 * Common Bootstrap components for scalajs-react
 */
object Bootstrap {

  trait BootstrapJQuery extends JQuery {
    def modal(action: String): BootstrapJQuery = js.native
    def modal(options: js.Any): BootstrapJQuery = js.native
  }

  implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]

  // Common Bootstrap contextual styles
  object CommonStyle extends Enumeration {
    val default, primary, success, info, warning, danger = Value
  }

  object Button {

    case class Props(onClick: () => Unit, style: CommonStyle.Value = CommonStyle.default, addClasses: String = "")

    val component = ReactComponentB[Props]("Button")
      .render { (P, C) =>
      <.button(^.className := s"btn btn-${P.style} ${P.addClasses}", ^.tpe := "button", ^.onClick --> P.onClick())(C)
    }.build

    def apply(props: Props, children: ReactNode*) = component(props, children)
    def apply() = component
  }

  object Panel {

    case class Props(heading: String, style: CommonStyle.Value = CommonStyle.default)

    val component = ReactComponentB[Props]("Panel")
      .render { (P, C) =>
      <.div(^.className := s"panel panel-${P.style}")(
        <.div(^.className := "panel-heading")(P.heading),
        <.div(^.className := "panel-body")(C)
      )
    }.build

    def apply(props: Props, children: ReactNode*) = component(props, children)
    def apply() = component
  }

  object Modal {

    // header and footer are functions, so that they can get access to the the hide() function for their buttons
    case class Props(header: (Backend) => ReactNode, footer: (Backend) => ReactNode, closed: () => Unit, backdrop: Boolean = true,
                     keyboard: Boolean = true)

    class Backend(t: BackendScope[Props, Unit]) {
      def hide(): Unit = {
        // instruct Bootstrap to hide the modal
        jQuery(t.getDOMNode()).modal("hide")
      }

      // jQuery event handler to be fired when the modal has been hidden
      def hidden(e: JQueryEventObject): js.Any = {
        // inform the owner of the component that the modal was closed/hidden
        t.props.closed()
      }
    }

    val component = ReactComponentB[Props]("Modal")
      .stateless
      .backend(new Backend(_))
      .render((P, C, _, B) => {
      <.div(^.className := "modal fade", ^.role := "dialog", ^.aria.hidden := true,
        <.div(^.className := "modal-dialog",
          <.div(^.className := "modal-content",
            <.div(^.className := "modal-header", P.header(B)),
            <.div(^.className := "modal-body", C),
            <.div(^.className := "modal-footer", P.footer(B))
          )
        )
      )
    })
      .componentDidMount(scope => {
      val P = scope.props
      // instruct Bootstrap to show the modal
      jQuery(scope.getDOMNode()).modal(js.Dynamic.literal("backdrop" -> P.backdrop, "keyboard" -> P.keyboard, "show" -> true))
      // register event listener to be notified when the modal is closed
      jQuery(scope.getDOMNode()).on("hidden.bs.modal", null, null, scope.backend.hidden _)
    })
      .build

    def apply(props: Props, children: ReactNode*) = component(props, children)
    def apply() = component
  }

}
