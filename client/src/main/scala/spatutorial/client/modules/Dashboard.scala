package spatutorial.client.modules

import diode.react._
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.SPAMain.{Loc, TodoLoc}
import spatutorial.client.components._
import spatutorial.client.services.RootModel

object Dashboard {

  case class Props(ctl: RouterCtl[Loc], cm: ComponentModel[RootModel])

  // create the React component for Dashboard
  val component = ReactComponentB[Props]("Dashboard")
    .render_P { case Props(ctl, cm) => {
      // create dummy data for the chart
      val cp = Chart.ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
      <.div(
        // header, MessageOfTheDay and chart components
        <.h2("Dashboard"),
        cm.connect(_.motd)(Motd(_)),
        Chart(cp),
        // create a link to the Todo view
        <.div(ctl.link(TodoLoc)("Check your todos!"))
      )
    }
    }.build
}
