package spatutorial.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._
import spatutorial.shared._

object TodoList {

  case class TodoListProps(items: Seq[TodoItem], stateChange: (TodoItem) => Unit)

  val TodoList = ReactComponentB[TodoListProps]("TodoList")
    .render(P => {
    def renderItem(item: TodoItem) = {
      // convert priority into Bootstrap style
      val priority = item.priority match {
        case TodoLow => "list-group-item-info"
        case TodoNormal => ""
        case TodoHigh => "list-group-item-danger"
      }
      li(cls := s"list-group-item $priority")(
        input(`type` := "checkbox", checked := item.completed, onChange --> P.stateChange(item.copy(completed = !item.completed))),
        if(item.completed) s(item.content) else span(item.content)
      )
    }
    ul(cls := "list-group")(P.items map renderItem)
  })
    .build

  def apply(props: TodoListProps) = TodoList(props)
}
