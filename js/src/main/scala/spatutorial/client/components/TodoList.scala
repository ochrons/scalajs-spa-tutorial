package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Bootstrap.Button
import spatutorial.shared._

object TodoList {

  case class TodoListProps(items: Seq[TodoItem], stateChange: (TodoItem) => Unit, editItem: (TodoItem) => Unit, deleteItem: (TodoItem) => Unit)

  val TodoList = ReactComponentB[TodoListProps]("TodoList")
    .render(P => {
    def renderItem(item: TodoItem) = {
      // convert priority into Bootstrap style
      val priority = item.priority match {
        case TodoLow => "list-group-item-info"
        case TodoNormal => ""
        case TodoHigh => "list-group-item-danger"
      }
      <.li(^.className := s"list-group-item $priority")(
        <.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
        <.span(" "),
        if (item.completed) <.s(item.content) else <.span(item.content),
        Button(Button.Props(() => P.editItem(item), addClasses = "pull-right btn-xs"), "Edit"),
        Button(Button.Props(() => P.deleteItem(item), addClasses = "pull-right btn-xs"), "Delete")
      )
    }
    <.ul(^.className := "list-group")(P.items map renderItem)
  })
    .build

  def apply(props: TodoListProps) = TodoList(props)
}
