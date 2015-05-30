package spatutorial.client.services

import java.nio.ByteBuffer

import boopickle._
import org.scalajs.dom

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.typedarray._

object AjaxClient extends autowire.Client[ByteBuffer, Unpickler, Pickler] {

  import org.scalajs.dom.ext.AjaxException
  import scala.scalajs.js.typedarray.TypedArrayBufferOps._

  // Scala.js DOM 0.8.0 does not support binary data, so we implement this here
  def post(url: String,
           data: ByteBuffer,
           timeout: Int = 0,
           headers: Map[String, String] = Map.empty,
           withCredentials: Boolean = false,
           responseType: String = "") = {

    val req = new dom.XMLHttpRequest()
    val promise = Promise[dom.XMLHttpRequest]()

    req.onreadystatechange = { (e: dom.Event) =>
      if (req.readyState == 4) {
        if ((req.status >= 200 && req.status < 300) || req.status == 304)
          promise.success(req)
        else
          promise.failure(AjaxException(req))
      }
    }
    req.open("POST", url)
    req.responseType = responseType
    req.timeout = timeout
    req.withCredentials = withCredentials
    headers.foreach(x => req.setRequestHeader(x._1, x._2))
    req.send(data.typedArray())
    promise.future
  }

  override def doCall(req: Request): Future[ByteBuffer] = {
    post(url = "/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))

    /*
        // Scala.js DOM 0.8.1 supports binary data, earlier versions don't
        dom.ext.Ajax.post(
          url = "/api/" + req.path.mkString("/"),
          data = Pickle.intoBytes(req.args),
          responseType = "arraybuffer",
          headers = Map("Content-Type" -> "application/octet-stream")
        ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
    */
  }

  def read[Result: Unpickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
  def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}
