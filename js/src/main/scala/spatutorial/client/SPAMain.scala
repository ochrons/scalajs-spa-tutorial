package spatutorial.client

import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router.BaseUrl
import org.scalajs.dom
import spatutorial.client.modules.MainRouter
import spatutorial.client.logger._
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

    // build a baseUrl, this method works for both local and server addresses (assuming you use #)
    val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
    val router = MainRouter.router(baseUrl)

    // tell React to render the router in the document body
    React.render(router(), dom.document.body)
  }
}
