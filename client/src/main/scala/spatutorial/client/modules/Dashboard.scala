package spatutorial.client.modules

import diode.data.Pot
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{Loc, TodoLoc}
import spatutorial.client.components._

import scala.util.Random

object Dashboard {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]])

  // create dummy data for the chart
  val cp = Chart.ChartProps(
    "Test chart",
    Chart.BarChart,
    ChartData(
      Random.alphanumeric.map(_.toUpper.toString).distinct.take(10),
      Seq(ChartDataset(Iterator.continually(Random.nextDouble() * 10).take(10).toSeq, "Data1"))
    )
  )

  class Backend($: BackendScope[Props, Unit]) {
    var motdWrapper: ReactComponentC.ReqProps[(ModelProxy[Pot[String]]) => ReactElement, Pot[String], _, TopNode] = _

    def render(props: Props) = {
      <.div(
        // header, MessageOfTheDay and chart components
        <.h2("Dashboard"),
        motdWrapper(Motd(_)),
        Chart(cp),
        // create a link to the To Do view
        <.div(props.router.link(TodoLoc)("Check your todos!"))
      )
    }

    def willMount(props: Props) = Callback {
      // use connect from ModelProxy to give Motd only partial view to the model
      motdWrapper = props.proxy.connect(m => m)
    }
  }

  // create the React component for Dashboard
  private val component = ReactComponentB[Props]("Dashboard")
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.props))
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]]) = component(Props(router, proxy))
}
