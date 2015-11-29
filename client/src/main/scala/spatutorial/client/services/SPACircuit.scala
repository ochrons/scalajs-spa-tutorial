package spatutorial.client.services

import autowire._
import diode._
import diode.util.RunAfterJS
import diode.react.ReactConnector
import diode.util._
import spatutorial.shared.{TodoItem, Api}
import boopickle.Default._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

// Actions
case object RefreshTodos

case class UpdateAllTodos(todos: Seq[TodoItem])

case class UpdateTodo(item: TodoItem)

case class DeleteTodo(item: TodoItem)

case class UpdateMotd(value: Pot[String] = Empty) extends PotAction[String, UpdateMotd] {
  override def next(value: Pot[String]) = UpdateMotd(value)
}

// The base model of our application
case class RootModel(todos: Pot[Todos], motd: Pot[String])

case class Todos(items: Seq[TodoItem]) {
  def updated(newItem: TodoItem) = {
    items.indexWhere(_.id == newItem.id) match {
      case -1 =>
        // add new
        Todos(items :+ newItem)
      case idx =>
        // replace old
        Todos(items.updated(idx, newItem))
    }
  }
  def remove(item: TodoItem) = Todos(items.filterNot(_ == item))
}

/**
  * Handles actions related to todos
  * @param modelRW Reader/Writer to access the model
  * @tparam M
  */
class TodoHandler[M](modelRW: ModelRW[M, Pot[Todos]]) extends ActionHandler(modelRW) {
  override def handle = {
    case RefreshTodos =>
      val updateServer = () => AjaxClient[Api].getTodos().call().map(UpdateAllTodos)
      effectOnly(updateServer)
    case UpdateAllTodos(todos) =>
      // got new todos, update model
      updated(Ready(Todos(todos)))
    case UpdateTodo(item) =>
      val updateServer = () => AjaxClient[Api].updateTodo(item).call().map(UpdateAllTodos)
      // make a local update and inform server
      updated(value.map(_.updated(item)), updateServer)
    case DeleteTodo(item) =>
      val updateServer = () => AjaxClient[Api].deleteTodo(item.id).call().map(UpdateAllTodos)
      // make a local update and inform server
      updated(value.map(_.remove(item)), updateServer)
  }
}

/**
  * Handles actions related to the Motd
  * @param modelRW Reader/Writer to access the model
  * @tparam M
  */
class MotdHandler[M](modelRW: ModelRW[M, Pot[String]]) extends ActionHandler(modelRW) {
  implicit val runner = new RunAfterJS

  override def handle = {
    case action: UpdateMotd =>
      val updateF = action.effect(AjaxClient[Api].welcome("User X").call())(identity)
      action.handleWith(this, updateF)(PotAction.handler(Retry(3)))
  }
}

// Application circuit
object SPACircuit extends Circuit[RootModel] with ReactConnector[RootModel] {
  // initial application model
  override protected var model = RootModel(Empty, Empty)
  // combine all handlers into one
  override protected val actionHandler = combineHandlers(
    new TodoHandler(zoomRW(_.todos)((m, v) => m.copy(todos = v))),
    new MotdHandler(zoomRW(_.motd)((m, v) => m.copy(motd = v)))
  )
}