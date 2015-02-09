package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._

/**
 * Common Bootstrap components for scalajs-react
 */
object Bootstrap {

   object CommonStyle extends Enumeration {
     val default, primary, success, info, warning, danger = Value
   }
   /* Button */
   case class ButtonProps(onClick: () => Unit, style: CommonStyle.Value = CommonStyle.default)

   val Button = ReactComponentB[ButtonProps]("Button")
     .render { (P, C) =>
     button(cls := s"btn btn-${P.style}", `type` := "button", onClick --> P.onClick())(C)
   }.build

  /* Panel */
  case class PanelProps(heading:String, style: CommonStyle.Value = CommonStyle.default)

  val Panel = ReactComponentB[PanelProps]("Panel")
    .render { (P, C) =>
    div(cls := s"panel panel-${P.style}")(
      div(cls := "panel-heading")(P.heading),
      div(cls := "panel-body")(C)
    )
  }.build

}
