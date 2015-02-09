package spatutorial.shared

sealed trait TodoPriority

case object TodoLow extends TodoPriority
case object TodoNormal extends TodoPriority
case object TodoHigh extends TodoPriority

case class TodoItem(id:String, content:String, priority:TodoPriority, completed:Boolean)
