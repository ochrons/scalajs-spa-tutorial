package spatutorial.client.ukko

trait EventType

case object ChangeEvent extends EventType

trait EventEmitter[A] {
  type EventListener = (EventType, A) => Unit

  private var listeners = Map.empty[EventType, List[EventListener]]

  private def listenersForType(eType: EventType) = listeners.getOrElse(eType, Nil)

  /**
   * Adds a new listener for event type `eType`.
   * @param eType
   * @param listener
   * @return A function to remove the registered listener
   */
  def addListener(eType: EventType, listener: EventListener): () => Unit = {
    listeners += eType -> (listener :: listenersForType(eType))
    // we need to return a function for removing this particular listener, because
    // Scala instantiates new function objects when passing function types
    () => listeners += eType -> listenersForType(eType).filterNot(_ == listener)
  }

  def emitEvent(eType: EventType, source:A): Unit = {
    // call all listeners
    listenersForType(eType).foreach(_(eType, source))
  }
}
