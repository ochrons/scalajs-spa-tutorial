# Dashboard

[Dashboard module](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/modules/Dashboard.scala) is really simple 
in terms of React components as it contains no internal state nor backend functionality. It's basically just a placeholder for two other components 
`Motd` and `Chart`. The only method is the `render` method which is responsible for rendering the component when it's mounted by React. It also provides 
fake data for the Chart component, to keep simple.

```scala
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
```

## Message of the day

[Motd component](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/components/Motd.scala) is a simple React component that fetches *Message of the day*
from the server and displays it in a panel. The component consists of three different classes: `State` for storing component state, `Backend`
for functionality and `Motd` for instantiating the component. The `Motd` is not given any properties, so the prop type is `Unit`.

```scala
val Motd = ReactComponentB[Unit]("Motd")
  .initialState(State("loading...")) // show a loading text while message is being fetched from the server
  .backend(new Backend(_))
  .render((_, S, B) => {
    Panel(Panel.Props("Message of the day"), div(S.message),
      Button(Button.Props(B.refresh, CommonStyle.danger), Icon.refresh, span(" Update"))
    )
  })
  .componentDidMount(scope => {
    scope.backend.refresh()
  })
  .buildU
```

A React component is defined through a series of function calls. Each of these calls, like `initialState` modifies the type of the component,
meaning you cannot access the state in the `render` method unless you have initialized it with `initialState`.

In the Todo component earlier, we didn't define state nor backend and therefore the render method had only access to the properties. In this
`render` method we have access to all three, denoted by `_` (props), `S` (state) and `B` (backend). Since the component has no properties
(type `Unit`) we discard the first parameter using the `_` notation. The render method builds a simple hierarchy of other React components
(`Panel` and `Button`) containing the message from the server.

Of course to actually get the message, we need to request it from the server. To do this automatically when the component is mounted, we hook
a call to `refresh()` in the `componentDidMount` method. The `Backend` class takes care of the actual server Ajax call and after receiving a
response, updates the component state.

```scala
def refresh() {
  // load a new message from the server
  AjaxClient[Api].motd("User X").call().foreach { message =>
    t.modState(_ => State(message))
  }
}
```

How the magic of calling the server actually happens is covered in a [later chapter](autowire-and-boopickle.md).

## Links to other routes

Sometimes you need to create a link that takes the user to an another module behind a route. To create these links in a type-safe manner,
the tutorial passes an instance of `RouterCtl` to components.

```scala
ctl.link(TodoLoc)("Check your todos!")
```

