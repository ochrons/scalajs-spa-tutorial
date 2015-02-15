package spatutorial.client.modules

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Bootstrap._
import spatutorial.client.components.TodoList.TodoListProps
import spatutorial.client.components._
import spatutorial.client.services.AjaxClient
import spatutorial.shared._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object Todo {

  case class TodoState(items: Seq[TodoItem] = Seq(), showTodoForm: Boolean = false)

  class Backend(t: BackendScope[_, TodoState]) {
    def updateTodo(item: TodoItem): Unit = {
      // inform the server about this update
      AjaxClient[Api].updateTodo(item).call().foreach { _ =>
        // get new todos after server has updated
        refresh()
      }
    }

    def refresh(): Unit = {
      // load Todos from the server
      AjaxClient[Api].getTodos().call().foreach { todos =>
        t.modState(_ => TodoState(todos))
      }
    }

    def addTodo(): Unit = {
      t.modState(s => s.copy(showTodoForm = true))
    }

    def todoEdited(item: TodoItem, cancelled: Boolean): Unit = {
      if (cancelled) {
        // nothing to do here
        println("Todo editing cancelled")
      } else {
        println(s"Todo edited: $item")
        updateTodo(item)
      }
      t.modState(s => s.copy(showTodoForm = false))
    }
  }

  // create the React component for ToDo management
  val component = ReactComponentB[MainRouter.Router]("TODO")
    .initialState(TodoState()) // initial state is an empty list
    .backend(new Backend(_))
    .render((router, S, B) => {
    Panel(Panel.Props("What needs to be done"), TodoList(TodoListProps(S.items, B.updateTodo)),
      Button(Button.Props(B.addTodo), Icon.plusSquare, " New"),
      // if modal is open, add it to the panel
      if (S.showTodoForm) TodoForm(TodoForm.Props(None, B.todoEdited))
      else // otherwise add a placeholder
        Seq.empty[ReactElement])
  })
    .componentDidMount(_.backend.refresh())
    .build
}

object TodoForm {

  case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) => Unit)

  case class State(item: TodoItem, cancelled: Boolean = true)

  class Backend(t: BackendScope[Props, State]) {
    def submitForm(): Unit = {
      // mark it as NOT cancelled (which is the default)
      t.modState(s => s.copy(cancelled = false))
    }

    def formClosed(): Unit = {
      // call parent handler with the new item and whether form was OK or cancelled
      t.props.submitHandler(t.state.item, t.state.cancelled)
    }

    def updateDescription(e: ReactEventI) = {
      // update TodoItem content
      t.modState(s => s.copy(item = s.item.copy(content = e.currentTarget.value)))
    }

    def updatePriority(e: ReactEventI) = {
      // update TodoItem priority
      val newPri = e.currentTarget.value match {
        case p if p == TodoHigh.toString => TodoHigh
        case p if p == TodoNormal.toString => TodoNormal
        case p if p == TodoLow.toString => TodoLow
      }
      t.modState(s => s.copy(item = s.item.copy(priority = newPri)))
    }
  }

  val component = ReactComponentB[Props]("TodoForm")
    .initialStateP(p => State(p.item.getOrElse(TodoItem("", "", TodoNormal, false))))
    .backend(new Backend(_))
    .render((P, S, B) => {
    Modal(Modal.Props(
      // header contains a cancel button (X)
      header = be => <.span(<.button(^.tpe := "button", ^.className := "close", ^.onClick --> be.hide(), Icon.close), <.h4("Add new todo")),
      // footer has the OK button
      footer = be => <.span(Button(Button.Props(() => {B.submitForm(); be.hide()}), "OK")),
      // this is called after the modal has been hidden (animation is completed)
      closed = B.formClosed),
      <.div(^.className := "form-group",
        <.label(^.`for` := "description", "Description"),
        <.input(^.tpe := "text", ^.className := "form-control", ^.id := "description", ^.value := S.item.content,
          ^.placeholder := "write description", ^.onChange ==> B.updateDescription)),
      <.div(^.className := "form-group",
        <.label(^.`for` := "priority", "Priority"),
        // using defaultValue = "Normal" instead of option/selected due to React
        <.select(^.className := "form-control", ^.id := "priority", ^.value := S.item.priority.toString, ^.onChange ==> B.updatePriority,
          <.option(^.value := TodoHigh.toString, "High"),
          <.option(^.value := TodoNormal.toString, "Normal"),
          <.option(^.value := TodoLow.toString, "Low")
        )
      )
    )
  }).build

  def apply(props: Props) = component(props)
}