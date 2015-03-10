package spatutorial.client

import japgolly.scalajs.react.React
import org.scalajs.dom
import spatutorial.client.logger._
import spatutorial.client.modules.MainRouter

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport("SPAMain")
object SPAMain extends js.JSApp {
  @JSExport
  def main(): Unit = {
    log.warn("Application starting")
    // send log messages also to the server
    log.enableServerLogging("/logging")
    log.info("This message goes to server as well")

    // tell React to render the router in the document body
    React.render(MainRouter.routerComponent(), dom.document.body)
  }
}
