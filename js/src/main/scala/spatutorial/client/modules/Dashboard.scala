package spatutorial.client.modules

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.components.Chart.ChartProps
import spatutorial.client.components._

object Dashboard {

  trait DashboardRoute extends BaseRoute {
    // create the React component for Dashboard
    val DashboardComponent = ReactComponentB[Router]("Dashboard")
      .render(router => {
      val cp = ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
      <.div(
        // just a header and MessageOfTheDay component
        <.h2("Dashboard"), Motd(), Chart(cp)
      )
    }).build

    // register the component and store location
    val dashboard: Loc = register(rootLocation(DashboardComponent))

    // register it for the Main Menu
    registerMenu(RouterMenuItem("Dashboard", Icon.dashboard, dashboard))
  }

}
