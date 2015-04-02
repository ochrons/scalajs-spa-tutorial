# Todo module and data flow

The Todo module and its React component are a bit more interesting than Dashboard as they provide some interaction. The module contains a `TodoList`
component, displaying a list of Todo items retrieved from the server. User can then click the checkbox next to the item to indicate if that item
is completed or not. Internally the state of Todo items is maintained by the `Todo` module and `TodoList` just passively displays them.

Before going into the details of the actual Todo module and related components, let's ponder a while about data flow in a Scala.js React application.

## Unidirectional data flow

Several JS frameworks out there (for example AngularJS) use mutable state and two-way data binding. In this tutorial, however, we are taking
cues from Facebook's [Flux](https://github.com/facebook/flux), which is an architecture for unidirectional data flow and immutable state. 
This architecture works especially well in more complex applications, where two-way data binding can quickly lead to all kinds of hard issues. 
It's also a relatively simple concept, so it works well even in a simple tutorial application like this. Below you can see a diagram of the
Flux architecture.

![Flux architecture](http://facebook.github.io/flux/img/flux-simple-f8-diagram-with-client-action-1300w.png)

It consists of a Dispatcher that takes in *Actions*, and dispatches them to all *Stores* that then inform
*Views* to update themselves with the new data. This kind of message dispatching sounds quite familiar if you've used actor frameworks
like [Akka](http://akka.io) before, so we might as well call those parts with a bit different names.

![Actor architecture](images/dispatcher-actor.png?raw=true)

It's not a real actor framework, for example the dispatcher sends all messages to all registered actors, but it's close enough for our
purposes. To explain how this works in practice, let's look at the concrete examples in the Todo module.

## Modifying a Todo state 

The `TodoList` component renders a checkbox for each Todo, which can be used to mark the Todo as *completed*.

```scala
<.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
```

Clicking the checkbox calls the `stateChange` method, which maps to `updateTodo` method in `TodoActions` object. The `TodoActions` is a
collection of functions to communicate with the server about changes to the todo list.

```scala
object TodoActions {
  def updateTodo(item: TodoItem) = {
    // inform the server to update/add the item
    AjaxClient[Api].updateTodo(item).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }
```

Note that nothing really happens on the client side as a result of clicking the checkbox. All it does is tell the server that the state
of completion has changed on the selected Todo. Only after the call to the server returns with an updated list of todos, is the client updated.
The update happens indirectly by sending an `UpdateAllTodos` message to the `MainDispatcher`. This message is then dispatched to all registered
actors, which in this case means only the `TodoStore`.

```scala
// refine a reactive variable
private val items = Var(Seq.empty[TodoItem])

private def updateItems(newItems: Seq[TodoItem]): Unit = {
  // check if todos have really changed
  if (newItems != items()) {
    // use Rx to update, which propagates down to dependencies
    items() = newItems
  }
}

override def receive = {
  case UpdateAllTodos(todos) =>
    updateItems(todos)
```

`TodoStore` updates its internal state with the new items (only if there is a real change) and this change is propagated using 
[ScalaRx](https://github.com/lihaoyi/scala.rx) to all interested parties. As it happens, there are
actually two separate components observing changes in the `TodoStore`. One is the Todo module where we started:

```scala
// get todos from the Rx defined in props
TodoList(TodoListProps(P.todos(), TodoActions.updateTodo, item => B.editTodo(Some(item)), B.deleteTodo))
```

It forces an update on the component, which in turn causes a call to `render` to refresh the view. Within the view, the new todos are fetched from
`TodoStore` using a reactive variable (`Rx`). This change cascades down to `TodoList` and to the individual Todo that was originally clicked. 
Mission accomplished!

But as we mentioned before, there was another component interested in changes to the Todos. This is the main menu item for Todo, which shows
the number of open Todos.

```scala
val todoCount = props.todos().count(!_.completed)
Seq(
  <.span("Todo "),
  if (todoCount > 0) <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount) else <.span()
)
```

This is the beauty of unidirectional data flow, where the components do not need to know where the change came from, or who might be
interested in the change. All state changes are propagated to interested parties automatically.

Next, let's look how to set up everything for data to flow.

## Wiring

![Wiring](images/control-flow.png?raw=true)

As you can see in the diagram above, there's all kinds of control flow activity going on in the application. Relevant classes are:

**MainDispatcher**
* Singleton instance of `Dispatcher` that everything is using

**TodoStore**
* A store implementing both `Actor` for receiving messages from `Dispatcher`. Provides reactive variables (`Rx`) to propagate changes.

**TodoActions**
* Helper class to make Ajax calls to the server and initiate updates via the dispatcher

`TodoStore` registers itself with the `MainDispatcher` in its singleton constructor.
```scala
MainDispatcher.register(this)
```

The `Todo` module and `MainMenu` register themselves, when they are mounted, as observers to changes in `TodoStore` items. 
They also initiate a refresh request for the todos.

```scala
def mounted(): Unit = {
  // hook up to TodoStore changes
  val obsTodos = t.props.todos.foreach { _ => t.forceUpdate() }
  onUnmount {
    // stop observing when unmounted
    obsTodos.kill()
  }
  // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
  MainDispatcher.dispatch(RefreshTodos)
}
```

The `foreach` call returns an `Obs` which we can use to observe changes. Whenever `todos` change, the function body is executed, forcing an update on
the React component. The actual value of `todos` is not used here, but within the view definition.
 
Note the use of [onUnmount](https://github.com/japgolly/scalajs-react/tree/master/extra#onunmount) to automatically remove the observer
when the component is unmounted.

Another, cleaner way to do the same is to use an observer pattern.

```scala
abstract class RxObserver[BS <: BackendScope[_,_]](scope: BS) extends OnUnmount {
  protected def observe[T](rx:Rx[T]): Unit = {
    val obs = rx.foreach( _ => scope.forceUpdate() )
    // stop observing when unmounted
    onUnmount(obs.kill())
  }
}

class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
  def mounted(): Unit = {
    // hook up to TodoStore changes
    observe(t.props.todos)
  }
```

This does the same thing as before but a bit more elegantly. The `RxObserver` trait is independent and can be used for all Backend classes that
have dependencies on reactive variables.

Finally views hook up different callbacks to `TodoActions` when something happens to the todos.

## Editing todos

For adding new Todo items, the user interface provides a button and a modal dialog box (using the `Modal` component described earlier). Editing
an existing item is performed by clicking an *Edit* button next to the todo description. Both actions open the same dialog. Finally you can also
delete a todo by clicking the *Delete* button.

`TodoForm` is a simple React component built around a `Modal` which allows users to edit an existing Todo item, or create a new one 
(there is no difference between these two from the component's point of view). It looks like this

![Dialog box](images/dialogbox.png?raw=true)

The dialog gets an optional item in properties and maintains current item in its state. The `submitHandler` callback is used to inform the parent
when the dialog box is closed (or cancelled).

```scala
case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) => Unit)

case class State(item: TodoItem, cancelled: Boolean = true)
```

Building the component looks a bit complicated, so let's walk through it.
```scala
val component = ReactComponentB[Props]("TodoForm")
  .initialStateP(p => State(p.item.getOrElse(TodoItem("", "", TodoNormal, false))))
  .backend(new Backend(_))
  .render((P, S, B) => {
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
```

State is first initialized with the provided item or with a new empty item. Within the `render` method a new `Modal` is created and in the properties we assign
couple of button controls. Note how both `header` and `footer` are actually functions that are given the `Modal`'s `Backend` so that they can call the `hide()`
function. In the OK button the state is first updated to _not cancelled_.

The form itself is quite straightforward, with handlers to update internal state as fields change. Note that with React the `select` element works a bit
differently from regular HTML5 and you must use `value` property to select the option instead of the typical `selected` attribute.

When the form closes, the parent's `submitHandler` gets called with the item and a flag indicating if the dialog box was cancelled.
```scala
def formClosed(): Unit = {
  // call parent handler with the new item and whether form was OK or cancelled
  t.props.submitHandler(t.state.item, t.state.cancelled)
}
```

But now it's time to get to the bottom of client-server communications!

