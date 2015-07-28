package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Bootstrap.{CommonStyle, Button}
import spatutorial.shared._
import scalacss.ScalaCssReact._

object TodoList {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

  case class TodoListProps(items: Seq[TodoItem], stateChange: (TodoItem) => Unit, editItem: (TodoItem) => Unit, deleteItem: (TodoItem) => Unit)

  val TodoList = ReactComponentB[TodoListProps]("TodoList")
    .render(P => {
    val style = bss.listGroup
    def renderItem(item: TodoItem) = {
      // convert priority into Bootstrap style
      val itemStyle = item.priority match {
        case TodoLow => style.itemOpt(CommonStyle.info)
        case TodoNormal => style.item
        case TodoHigh => style.itemOpt(CommonStyle.danger)
      }
      <.li(itemStyle)(
        <.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
        <.span(" "),
        if (item.completed) <.s(item.content) else <.span(item.content),
        Button(Button.Props(() => P.editItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Edit"),
        Button(Button.Props(() => P.deleteItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Delete")
      )
    }
    <.ul(style.listGroup)(P.items map renderItem)
  })
    .build

  def apply(props: TodoListProps) = TodoList(props)
}
