package spatutorial.server

import java.util.{UUID, Date}

import spatutorial.shared._

class ApiService extends Api {
  var todos = Seq(
    TodoItem("1", "Wear shirt that says “Life”. Hand out lemons on street corner.", TodoLow, false),
    TodoItem("2", "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, false),
    TodoItem("3", "Walk away slowly from an explosion without looking back.", TodoHigh, false),
    TodoItem("4", "Sneeze in front of the pope. Get blessed.", TodoNormal, true)
  )

  override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"

  override def getTodos(): Seq[TodoItem] = {
    // provide some fake Todos
    println(s"Sending ${todos.size} Todo items")
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Seq[TodoItem] = {
    // TODO, update database etc :)
    if(todos.exists(_.id == item.id)) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
      println(s"Todo item was updated: $item")
    } else {
      // add a new item
      val newItem = item.copy(id = UUID.randomUUID().toString)
      todos :+= newItem
      println(s"Todo item was added: $newItem")
    }
    todos
  }

  // delete a Todo
  override def deleteTodo(itemId: String): Seq[TodoItem] = {
    println(s"Deleting item with id = $itemId")
    todos = todos.filterNot(_.id == itemId)
    todos
  }
}
