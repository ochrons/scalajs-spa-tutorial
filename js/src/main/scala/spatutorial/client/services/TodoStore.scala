package spatutorial.client.services

import autowire._
import spatutorial.client.ukko._
import spatutorial.shared.{Api, TodoItem}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

case object RefreshTodos

case class UpdateAllTodos(todos: Seq[TodoItem])

case class UpdateTodo(item: TodoItem)

trait TodoStore extends Actor with EventEmitter[TodoStore] {
  override val name: String = "TodoStore"

  private case class State(items: Seq[TodoItem])

  private var state: State = State(Seq())

  override def receive = {
    case RefreshTodos =>
      // load all todos from the server
      AjaxClient[Api].getTodos().call().foreach { todos =>
        state = state.copy(items = todos)
        // inform listeners
        emitEvent(ChangeEvent, this)
      }
    case UpdateAllTodos(todos) =>
      state = state.copy(items = todos)
      // inform listeners
      emitEvent(ChangeEvent, this)
  }

  def todos = state.items
}

// create a singleton instance of TodoStore
object TodoStore extends TodoStore {
  // register this actor with the dispatcher
  MainDispatcher.register(this)
}

object TodoActions {
  def updateTodo(item: TodoItem) = {
    // inform the server to update/add the item
    AjaxClient[Api].updateTodo(item).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }

  def deleteTodo(item:TodoItem) = {
    // tell server to delete a todo
    AjaxClient[Api].deleteTodo(item.id).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }
}
