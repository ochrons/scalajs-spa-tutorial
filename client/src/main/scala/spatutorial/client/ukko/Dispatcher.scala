package spatutorial.client.ukko

import scala.collection.immutable.Queue

trait Dispatcher {

  import Dispatcher._

  protected var actors = Set.empty[Actor]
  protected var pendingActors = Set.empty[Actor]
  protected var handledActors = Set.empty[Actor]

  protected var messageQueue = Queue.empty[Any]

  def register(actor: Actor): Unit = {
    // register the actor
    actors += actor
  }

  def unregister(actor: Actor): Unit = {
    assert(actors.contains(actor), s"Actor ${actor.name} not registered")
    actors -= actor
  }

  def dispatch(msg: AnyRef): Unit = {
    messageQueue = messageQueue.enqueue(msg)
    if (isDispatching) {
      // currently dispatching, just add to the message queue
    } else {
      isDispatching = true
      try {
        while (messageQueue.nonEmpty) {
          // clear statuses
          pendingActors = Set.empty[Actor]
          handledActors = Set.empty[Actor]
          // send message to every registered actor
          actors.foreach { actor =>
            if (!pendingActors.contains(actor)) {
              invokeActor(actor, messageQueue.front)
            }
          }
          // remove from queue only after all actors have processed the message
          messageQueue = messageQueue.tail
        }
      } finally {
        isDispatching = false
      }
    }
  }

  def waitFor(actorsToWait: Actor*): Unit = {
    assert(isDispatching, "Must be dispatching when calling waitFor")

    actorsToWait.foreach { actor =>
      if (pendingActors.contains(actor)) {
        assert(handledActors.contains(actor), s"Circular dependency detected while waiting for ${actor.name}")
      } else {
        assert(actors.contains(actor))
        invokeActor(actor, messageQueue.front)
      }
    }
  }

  protected def invokeActor(actor: Actor, msg: Any): Unit = {
    pendingActors += actor
    actor.receiveD(this)(msg)
    handledActors += actor
  }
}

object Dispatcher {
  // all dispatchers share this bit of state to make sure they don't run at the same time
  private[Dispatcher] var isDispatching = false
}