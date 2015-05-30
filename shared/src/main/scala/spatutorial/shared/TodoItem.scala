package spatutorial.shared

import boopickle._

sealed trait TodoPriority

case object TodoLow extends TodoPriority

case object TodoNormal extends TodoPriority

case object TodoHigh extends TodoPriority

case class TodoItem(id: String, timeStamp:Int, content: String, priority: TodoPriority, completed: Boolean)

object TodoPriority {
  implicit val todoPriorityPickler = CompositePickler[TodoPriority].addConcreteType[TodoLow.type].addConcreteType[TodoNormal.type].addConcreteType[TodoHigh.type]
}
