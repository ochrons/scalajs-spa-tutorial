package spatutorial.client.ukko

trait Actor {
  type Receive = PartialFunction[Any, Unit]

  protected var dispatcher: Dispatcher = _

  def name: String

  /**
   * Called by dispatcher, so that the actor instance can store active dispatcher,
   * which is used by `waitFor`
   *
   * @param activeDispatcher Dispatcher that is making this call
   * @return
   */
  protected[ukko] def receiveD(activeDispatcher: Dispatcher): Receive = {
    dispatcher = activeDispatcher
    receive
  }

  /**
   * Handy shortcut to call dispatcher's `waitFor`
   * @param actorsToWait Actors we depend on, so make sure they process the message first
   */
  protected def waitFor(actorsToWait: Actor*) = dispatcher.waitFor(actorsToWait: _*)

  /**
   * Actors need to override this function to define their behavior
   *
   * @return `PartialFunction` defining actor behavior
   */
  def receive: Receive
}
