package spatutorial.client.modules

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Bootstrap._
import spatutorial.client.components.TodoList.TodoListProps
import spatutorial.client.components._
import spatutorial.client.services._
import spatutorial.client.ukko._
import spatutorial.shared._

object Todo {

  case class TodoState(items: Seq[TodoItem] = Seq(), selectedItem: Option[TodoItem] = None, showTodoForm: Boolean = false)

  class Backend(t: BackendScope[_, TodoState]) extends OnUnmount {
    def mounted(): Unit = {
      // listen to change events
      val removeListener = TodoStore.addListener(ChangeEvent, updated)
      // register things to do when unmounted
      onUnmount {
        removeListener()
      }
      // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
      MainDispatcher.dispatch(RefreshTodos)
    }

    def updated(event: EventType, store: TodoStore): Unit = {
      // get updated todos from the store
      t.modState(_.copy(items = store.todos))
    }

    def editTodo(item: Option[TodoItem]): Unit = {
      // activate the todo dialog
      t.modState(s => s.copy(selectedItem = item, showTodoForm = true))
    }

    def deleteTodo(item: TodoItem): Unit = {
      TodoActions.deleteTodo(item)
    }

    def todoEdited(item: TodoItem, cancelled: Boolean): Unit = {
      if (cancelled) {
        // nothing to do here
        println("Todo editing cancelled")
      } else {
        println(s"Todo edited: $item")
        TodoActions.updateTodo(item)
      }
      // hide the todo dialog
      t.modState(s => s.copy(showTodoForm = false))
    }
  }

  // create the React component for ToDo management
  val component = ReactComponentB[MainRouter.Router]("TODO")
    .initialState(TodoState()) // initial state is an empty list
    .backend(new Backend(_))
    .render((router, S, B) => {
    Panel(Panel.Props("What needs to be done"), TodoList(TodoListProps(S.items, TodoActions.updateTodo, item => B.editTodo(Some(item)), B.deleteTodo)),
      Button(Button.Props(() => B.editTodo(None)), Icon.plusSquare, " New"),
      // if the dialog is open, add it to the panel
      if (S.showTodoForm) TodoForm(TodoForm.Props(S.selectedItem, B.todoEdited))
      else // otherwise add an empty placeholder
        Seq.empty[ReactElement])
  })
    .componentDidMount(_.backend.mounted())
    .configure(OnUnmount.install)
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
    val headerText = if (S.item.id == "") "Add new todo" else "Edit todo"
    Modal(Modal.Props(
      // header contains a cancel button (X)
      header = be => <.span(<.button(^.tpe := "button", ^.className := "close", ^.onClick --> be.hide(), Icon.close), <.h4(headerText)),
      // footer has the OK button that submits the form before hiding it
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