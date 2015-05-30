package spatutorial.client.modules

import japgolly.scalajs.react.extra.router2.RouterCtl
import spatutorial.client.SPAMain.Loc

import scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._
import rx._
import rx.ops._
import spatutorial.client.components.Bootstrap._
import spatutorial.client.components.TodoList.TodoListProps
import spatutorial.client.components._
import spatutorial.client.logger._
import spatutorial.client.services._
import spatutorial.shared._

object Todo {

  case class Props(todos: Rx[Seq[TodoItem]], router: RouterCtl[Loc])

  case class State(selectedItem: Option[TodoItem] = None, showTodoForm: Boolean = false)

  abstract class RxObserver[BS <: BackendScope[_, _]](scope: BS) extends OnUnmount {
    protected def observe[T](rx: Rx[T]): Unit = {
      val obs = rx.foreach(_ => scope.forceUpdate())
      // stop observing when unmounted
      onUnmount(obs.kill())
    }
  }

  class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
    def mounted(): Unit = {
      // hook up to TodoStore changes
      observe(t.props.todos)
      // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
      MainDispatcher.dispatch(RefreshTodos)
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
        log.debug("Todo editing cancelled")
      } else {
        log.debug(s"Todo edited: $item")
        TodoActions.updateTodo(item)
      }
      // hide the todo dialog
      t.modState(s => s.copy(showTodoForm = false))
    }
  }

  // create the React component for ToDo management
  val component = ReactComponentB[Props]("TODO")
    .initialState(State()) // initial state from TodoStore
    .backend(new Backend(_))
    .render((P, S, B) => {
    Panel(Panel.Props("What needs to be done"), TodoList(TodoListProps(P.todos(), TodoActions.updateTodo, item => B.editTodo(Some(item)), B.deleteTodo)),
      Button(Button.Props(() => B.editTodo(None)), Icon.plusSquare, " New"),
      // if the dialog is open, add it to the panel
      if (S.showTodoForm) TodoForm(TodoForm.Props(S.selectedItem, B.todoEdited))
      else // otherwise add an empty placeholder
        Seq.empty[ReactElement])
  })
    .componentDidMount(_.backend.mounted())
    .configure(OnUnmount.install)
    .build

  /** Returns a function compatible with router location system while using our own props */
  def apply(store: TodoStore) = (router: RouterCtl[Loc]) => {
    component(Props(store.todos, router))
  }
}

object TodoForm {
  // shorthand for styles
  @inline private def bss = GlobalStyles.bootstrapStyles

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
    .initialStateP(p => State(p.item.getOrElse(TodoItem("", 0, "", TodoNormal, false))))
    .backend(new Backend(_))
    .render((P, S, B) => {
    log.debug(s"User is ${if (S.item.id == "") "adding" else "editing"} a todo")
    val headerText = if (S.item.id == "") "Add new todo" else "Edit todo"
    Modal(Modal.Props(
      // header contains a cancel button (X)
      header = be => <.span(<.button(^.tpe := "button", bss.close, ^.onClick --> be.hide(), Icon.close), <.h4(headerText)),
      // footer has the OK button that submits the form before hiding it
      footer = be => <.span(Button(Button.Props(() => {B.submitForm(); be.hide()}), "OK")),
      // this is called after the modal has been hidden (animation is completed)
      closed = B.formClosed),
      <.div(bss.formGroup,
        <.label(^.`for` := "description", "Description"),
        <.input(^.tpe := "text", bss.formControl, ^.id := "description", ^.value := S.item.content,
          ^.placeholder := "write description", ^.onChange ==> B.updateDescription)),
      <.div(bss.formGroup,
        <.label(^.`for` := "priority", "Priority"),
        // using defaultValue = "Normal" instead of option/selected due to React
        <.select(bss.formControl, ^.id := "priority", ^.value := S.item.priority.toString, ^.onChange ==> B.updatePriority,
          <.option(^.value := TodoHigh.toString, "High"),
          <.option(^.value := TodoNormal.toString, "Normal"),
          <.option(^.value := TodoLow.toString, "Low")
        )
      )
    )
  }).build

  def apply(props: Props) = component(props)
}