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
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Unit = {
    // TODO, update database etc :)
    println(s"Todo item was updated: $item")
    if(todos.exists(_.id == item.id)) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
    } else {
      // add a new item
      todos :+= item.copy(id = UUID.randomUUID().toString)
    }
  }
}
