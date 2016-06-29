package spatutorial.client.modules

import diode.data.Pot
import diode.react._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{Loc, TodoLoc}
import spatutorial.client.components._

import scala.util.Random
import scala.language.existentials

object Dashboard {

  case class Props(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]])

  case class State(motdWrapper: ReactConnectProxy[Pot[String]])

  // create dummy data for the chart
  val cp = Chart.ChartProps(
    "Test chart",
    Chart.BarChart,
    ChartData(
      Random.alphanumeric.map(_.toUpper.toString).distinct.take(10),
      Seq(ChartDataset(Iterator.continually(Random.nextDouble() * 10).take(10).toSeq, "Data1"))
    )
  )

  // create the React component for Dashboard
  private val component = ReactComponentB[Props]("Dashboard")
    // create and store the connect proxy in state for later use
    .initialState_P(props => State(props.proxy.connect(m => m)))
    .renderPS { (_, props, state) =>
      <.div(
        // header, MessageOfTheDay and chart components
        <.h2("Dashboard"),
        state.motdWrapper(Motd(_)),
        Chart(cp),
        // create a link to the To Do view
        <.div(props.router.link(TodoLoc)("Check your todos!"))
      )
    }
    .build

  def apply(router: RouterCtl[Loc], proxy: ModelProxy[Pot[String]]) = component(Props(router, proxy))
}
