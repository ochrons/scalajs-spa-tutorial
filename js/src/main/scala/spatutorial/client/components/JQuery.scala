package spatutorial.client.components

import org.scalajs.dom._

import scala.scalajs.js

/**
 * Minimal facade for JQuery. Use https://github.com/scala-js/scala-js-jquery/blob/master/src/main/scala/org/scalajs/jquery/JQuery.scala
 * for more complete one.
 */
trait JQueryEventObject extends Event {
  var data: js.Any = js.native
}

trait JQueryStatic extends js.Object {
  def apply(element: Element): JQuery = js.native
}

trait JQuery extends js.Object {
  def on(events: String, selector: js.Any, data: js.Any, handler: js.Function1[JQueryEventObject, js.Any]): JQuery = js.native
  def off(events: String): JQuery = js.native
}