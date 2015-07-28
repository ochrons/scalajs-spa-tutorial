package spatutorial.client.modules

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{TodoLoc, Loc}
import spatutorial.client.components._

object Dashboard {
  // create the React component for Dashboard
  val component = ReactComponentB[RouterCtl[Loc]]("Dashboard")
    .render(ctl => {
    // create dummy data for the chart
    val cp = Chart.ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
    <.div(
      // header, MessageOfTheDay and chart components
      <.h2("Dashboard"),
      Motd(),
      Chart(cp),
      // create a link to the Todo view
      <.div(ctl.link(TodoLoc)("Check your todos!"))
    )
  }).build
}
