package spatutorial.client

import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router.BaseUrl
import org.scalajs.dom
import spatutorial.client.modules.MainRouter

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("SPAMain")
object SPAMain extends JSApp {
  @JSExport
  def main(): Unit = {
    val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
    val router = MainRouter.router(baseUrl)

    React.render(router(), dom.document.body)
  }
}
