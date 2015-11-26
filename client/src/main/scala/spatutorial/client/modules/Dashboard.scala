package spatutorial.client.modules

import diode.react._
import diode.util.Pot
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{Loc, TodoLoc}
import spatutorial.client.components._
import spatutorial.client.services.RootModel

object Dashboard {

  case class Props(router: RouterCtl[Loc], cm: ComponentModel[Pot[String]])

  // create dummy data for the chart
  val cp = Chart.ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))

  // create the React component for Dashboard
  private val component = ReactComponentB[Props]("Dashboard")
    .render_P { case Props(router, cm) =>
      <.div(
        // header, MessageOfTheDay and chart components
        <.h2("Dashboard"),
        // use connect from ComponentModel to give Motd only partial view to the model
        cm.connect(m => m)(Motd(_)),
        Chart(cp),
        // create a link to the To Do view
        <.div(router.link(TodoLoc)("Check your todos!"))
      )
    }.build

  def apply(router: RouterCtl[Loc], cm: ComponentModel[Pot[String]]) = component(Props(router, cm))
}
