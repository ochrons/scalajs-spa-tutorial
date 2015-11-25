package spatutorial.client.modules

import japgolly.scalajs.react.extra.router.RouterCtl
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
      val obs = rx.foreach(_ => scope.forceUpdate.runNow())
      // stop observing when unmounted
      onUnmount(Callback(obs.kill()))
    }
  }

  class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
    def mounted(props: Props): Callback = Callback {
      // hook up to TodoStore changes
      observe(props.todos)
      // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
      MainDispatcher.dispatch(RefreshTodos)
    }

    def editTodo(item: Option[TodoItem]): Callback = {
      // activate the todo dialog
      t.modState(s => s.copy(selectedItem = item, showTodoForm = true))
    }

    def deleteTodo(item: TodoItem): Callback = {
      Callback(TodoActions.deleteTodo(item))
    }

    def todoEdited(item: TodoItem, cancelled: Boolean): Callback = {
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
    .renderPS(($, P, S) => {
      val B = $.backend
      Panel(Panel.Props("What needs to be done"), TodoList(TodoListProps(P.todos(),
        item => Callback(TodoActions.updateTodo(item)), item => B.editTodo(Some(item)), B.deleteTodo)),
        Button(Button.Props(B.editTodo(None)), Icon.plusSquare, " New"),
        // if the dialog is open, add it to the panel
        if (S.showTodoForm) TodoForm(TodoForm.Props(S.selectedItem, B.todoEdited))
        else // otherwise add an empty placeholder
          Seq.empty[ReactElement])
    })
    .componentDidMount(scope => scope.backend.mounted(scope.props))
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

  case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) => Callback)

  case class State(item: TodoItem, cancelled: Boolean = true)

  class Backend(t: BackendScope[Props, State]) {
    def submitForm(): Callback = {
      // mark it as NOT cancelled (which is the default)
      t.modState(s => s.copy(cancelled = false))
    }

    def formClosed(state: State, props: Props): Callback = {
      // call parent handler with the new item and whether form was OK or cancelled
      props.submitHandler(state.item, state.cancelled)
    }

    def updateDescription(e: ReactEventI) = {
      // update TodoItem content
      t.modState(s => s.copy(item = s.item.copy(content = e.target.value)))
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

    def render(s: State, p: Props) = {
      log.debug(s"User is ${if (s.item.id == "") "adding" else "editing"} a todo")
      val headerText = if (s.item.id == "") "Add new todo" else "Edit todo"
      Modal(Modal.Props(
        // header contains a cancel button (X)
        header = hide => <.span(<.button(^.tpe := "button", bss.close, ^.onClick --> hide, Icon.close), <.h4(headerText)),
        // footer has the OK button that submits the form before hiding it
        footer = hide => <.span(Button(Button.Props(submitForm() >> hide), "OK")),
        // this is called after the modal has been hidden (animation is completed)
        closed = () => formClosed(s, p)),
        <.div(bss.formGroup,
          <.label(^.`for` := "description", "Description"),
          <.input(^.tpe := "text", bss.formControl, ^.id := "description", ^.value := s.item.content,
            ^.placeholder := "write description", ^.onChange ==> updateDescription)),
        <.div(bss.formGroup,
          <.label(^.`for` := "priority", "Priority"),
          // using defaultValue = "Normal" instead of option/selected due to React
          <.select(bss.formControl, ^.id := "priority", ^.value := s.item.priority.toString, ^.onChange ==> updatePriority,
            <.option(^.value := TodoHigh.toString, "High"),
            <.option(^.value := TodoNormal.toString, "Normal"),
            <.option(^.value := TodoLow.toString, "Low")
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("TodoForm")
    .initialState_P(p => State(p.item.getOrElse(TodoItem("", 0, "", TodoNormal, false))))
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)
}