package spatutorial.client.modules

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Chart.ChartProps
import spatutorial.client.components._

object Dashboard {
  // create the React component for Dashboard
  val component = ReactComponentB[MainRouter.Router]("Dashboard")
    .render(router => {
    // create dummy data for the chart
    val cp = ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
    // get internal links
    val appLinks = MainRouter.appLinks(router)
    <.div(
      // header, MessageOfTheDay and chart components
      <.h2("Dashboard"),
      Motd(),
      Chart(cp),
      // create a link to the Todo view
      <.div(appLinks.todo("Check your todos!"))
    )
  }).build
}
