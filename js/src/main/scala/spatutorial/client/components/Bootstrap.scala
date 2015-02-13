package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Common Bootstrap components for scalajs-react
 */
object Bootstrap {

  // Common Bootstrap contextual styles
  object CommonStyle extends Enumeration {
    val default, primary, success, info, warning, danger = Value
  }

  /* Button */
  case class ButtonProps(onClick: () => Unit, style: CommonStyle.Value = CommonStyle.default)

  val Button = ReactComponentB[ButtonProps]("Button")
    .render { (P, C) =>
    <.button(^.className := s"btn btn-${P.style}", ^.tpe := "button", ^.onClick --> P.onClick())(C)
  }.build

  /* Panel */
  case class PanelProps(heading: String, style: CommonStyle.Value = CommonStyle.default)

  val Panel = ReactComponentB[PanelProps]("Panel")
    .render { (P, C) =>
    <.div(^.className := s"panel panel-${P.style}")(
      <.div(^.className := "panel-heading")(P.heading),
      <.div(^.className := "panel-body")(C)
    )
  }.build
}
