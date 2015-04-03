package spatutorial.server

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import spatutorial.shared.Api
import spray.http._
import spray.routing.SimpleRoutingApp

import scala.util.Properties

object Router extends autowire.Server[String, upickle.Reader, upickle.Writer] {
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

object Config {
  val c = ConfigFactory.load().getConfig("spatutorial")

  val productionMode = c.getBoolean("productionMode")
}

object MainApp extends SimpleRoutingApp {
  def main(args: Array[String]): Unit = {
    // create an Actor System
    implicit val system = ActorSystem("SPA")
    // use system's dispatcher as ExecutionContext for futures etc.
    implicit val context = system.dispatcher

    val port = Properties.envOrElse("SPA_PORT", "8080").toInt

    val apiService = new ApiService

    startServer("0.0.0.0", port = port) {
      get {
        pathSingleSlash {
          // serve the main page
          if (Config.productionMode)
            getFromResource("web/index-full.html")
          else
            getFromResource("web/index.html")
        } ~ pathPrefix("srcmaps") {
          if (!Config.productionMode)
            getFromDirectory("../")
          else
            complete(StatusCodes.NotFound)
        } ~
          // serve other requests directly from the resource directory
          getFromResourceDirectory("web")
      } ~ post {
        path("api" / Segments) { s =>
          extract(_.request.entity.asString) { e =>
            ctx =>
              // handle API requests via autowire
              val result = Router.route[Api](apiService)(
                autowire.Core.Request(s, upickle.read[Map[String, String]](e))
              )
              // force the use of application/json content type
              result.map(json => ctx.complete(HttpEntity(ContentTypes.`application/json`, json)))
          }
        } ~ path("logging") {
          entity(as[String]) { msg =>
            ctx =>
              println(s"ClientLog: $msg")
              ctx.complete(StatusCodes.OK)
          }
        }
      }
    }
  }
}
