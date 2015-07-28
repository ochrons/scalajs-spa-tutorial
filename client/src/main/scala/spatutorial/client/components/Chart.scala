package spatutorial.client.components

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import org.scalajs.dom.raw.HTMLCanvasElement

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName

trait ChartDataset extends js.Object {
  def label: String = js.native
  def fillColor: String = js.native
  def strokeColor: String = js.native
  def data: js.Array[Double] = js.native
}

object ChartDataset {
  def apply(data: Seq[Double], label: String, fillColor: String = "#8080FF", strokeColor: String = "#404080"): ChartDataset = {
    js.Dynamic.literal(
      data = data.toJSArray,
      label = label,
      fillColor = fillColor,
      strokeColor = strokeColor
    ).asInstanceOf[ChartDataset]
  }
}

trait ChartData extends js.Object {
  def labels: js.Array[String] = js.native
  def datasets: js.Array[ChartDataset] = js.native
}

object ChartData {
  def apply(labels: Seq[String], datasets: Seq[ChartDataset]): ChartData = {
    js.Dynamic.literal(
      labels = labels.toJSArray,
      datasets = datasets.toJSArray
    ).asInstanceOf[ChartData]
  }
}

// define a class to access the Chart.js component
@JSName("Chart")
class JSChart(ctx: js.Dynamic) extends js.Object {
  // create different kinds of charts
  def Line(data: ChartData): js.Dynamic = js.native
  def Bar(data: ChartData): js.Dynamic = js.native
}

object Chart {

  // avaiable chart styles
  sealed trait ChartStyle

  case object LineChart extends ChartStyle

  case object BarChart extends ChartStyle

  case class ChartProps(name: String, style: ChartStyle, data: ChartData, width: Int = 400, height: Int = 200)

  class Backend(t: BackendScope[ChartProps, _])

  val Chart = ReactComponentB[ChartProps]("Chart")
    .render((P) => {
    <.canvas(^.width := P.width, ^.height := P.height)
  }).componentDidMount(scope => {
    // access context of the canvas
    val ctx = scope.getDOMNode().asInstanceOf[HTMLCanvasElement].getContext("2d")
    // create the actual chart using the 3rd party component
    scope.props.style match {
      case LineChart => new JSChart(ctx).Line(scope.props.data)
      case BarChart => new JSChart(ctx).Bar(scope.props.data)
      case _ => throw new IllegalArgumentException
    }
  }).build

  def apply(props: ChartProps) = Chart(props)
}
