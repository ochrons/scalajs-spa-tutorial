package spatutorial.client.ukko

import utest._

object DispatcherTests extends TestSuite {

  class TestDispatcher extends Dispatcher {
    def actorCount = actors.size
  }

  abstract class TestActor(val name: String) extends Actor

  override def tests = TestSuite {
    'register {
      val actor = new TestActor("actor") {
        override def receive: Receive = {
          case _ =>
        }
      }
      val dispatcher = new TestDispatcher
      dispatcher.register(actor)
      assert(dispatcher.actorCount == 1)
      'unregister {
        dispatcher.unregister(actor)
        assert(dispatcher.actorCount == 0)
        'unregisterAgain {
          intercept[java.lang.AssertionError] {
            dispatcher.unregister(actor)
          }
        }
      }
    }

    'dispatch {
      var state = 0
      val actor = new TestActor("actor") {
        override def receive: Receive = {
          case "next" => state = state + 1
        }
      }
      val dispatcher = new TestDispatcher
      dispatcher.register(actor)
      dispatcher.dispatch("next")
      assert(state == 1)
    }

    'waitFor {
      var state = ""
      // create three actors that have dependencies
      val actor1 = new TestActor("actor1") {
        override def receive: Receive = {
          case "next" =>
            state += "First"
        }
      }
      val actor2 = new TestActor("actor2") {
        override def receive: Receive = {
          case "next" =>
            waitFor(actor1)
            state += "Second"
        }
      }
      val actor3 = new TestActor("actor3") {
        override def receive: Receive = {
          case "next" =>
            waitFor(actor1, actor2)
            state += "Third"
        }
      }
      val dispatcher = new TestDispatcher
      Seq(actor1, actor2, actor3).foreach(dispatcher.register)
      dispatcher.dispatch("next")
      assert(state == "FirstSecondThird")

      'isDispatching {
      }
      'circularDependency {
        // create a waitFor dependency between the actors
        var actor3: Actor = null
        val actor1 = new TestActor("actor1") {
          override def receive: Receive = {
            case "next" =>
              state += "First"
              waitFor(actor3)
          }
        }
        val actor2 = new TestActor("actor2") {
          override def receive: Receive = {
            case "next" =>
              waitFor(actor1)
              state += "Second"
          }
        }
        actor3 = new TestActor("actor3") {
          override def receive: Receive = {
            case "next" =>
              waitFor(actor2)
              state += "Third"
          }
        }
        val dispatcher = new TestDispatcher
        Seq(actor1, actor2, actor3).foreach(dispatcher.register)
        intercept[java.lang.AssertionError] {
          dispatcher.dispatch("next")
        }
      }
    }

    'messageQueue {
      var state = ""
      val actor = new TestActor("actor") {
        override def receive: Receive = {
          case "next" =>
            // dispatch a new message right away, which should be queued
            dispatcher.dispatch("last")
            state = "First"
          case "last" =>
            state += "Last"
        }
      }
      val dispatcher = new TestDispatcher
      dispatcher.register(actor)
      dispatcher.dispatch("next")
      assert(state == "FirstLast")
    }
  }
}
