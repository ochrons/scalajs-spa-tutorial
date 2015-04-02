# Integrating JavaScript components

Although Scala.js provides a superb environment for developing web clients, sometimes it makes sense to utilize the hard work of
thousands of JavaScript developers fumbling in the shadows :)

## Bootstrap CSS components

[Bootstrap](http://getbootstrap.com/) is a popular HTML/CSS/JS framework by Twitter for developing responsive applications. It comes with a lot
of styled HTML/CSS components that are easy to use and integrate into your application. Lot of Bootstrap actually doesn't even use JavaScript, all
the magic happens in CSS.

This tutorial wraps couple of simple Bootstrap [components](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/components/Bootstrap.scala)
(button and panel) into React components. Bootstrap uses contextual styles in many components to convey additional meaning. These can be
easily represented by a Scala enumeration.

```scala
object CommonStyle extends Enumeration {
  val default, primary, success, info, warning, danger = Value
}
```

To define an interactive button, it has to know what to do when it's clicked. In this example we simply give a function in the properties
alongside the contextual style. Note that the actual button contents doesn't need to be provided in the properties as it's more convenient
to define it through child component(s).

```scala
object Button {
  case class Props(onClick: () => Unit, style: CommonStyle.Value = CommonStyle.default)

  val component = ReactComponentB[Props]("Button")
    .render { (P, C) =>
    <.button(bss.buttonOpt(P.style), P.addStyles, ^.tpe := "button", ^.onClick --> P.onClick())(C)
  }.build

  def apply(props: Props, children: ReactNode*) = component(props, children)
  def apply() = component
}
```

This time the render method gets two parameters, the properties and the children given to this component. It simply renders a normal
button using Bootstrap CSS and binding `onClick` to the handler we defined in the properties. Finally the children are rendered within
the button tag.

Defining a Bootstrap Panel is about as simple.

```scala
object Panel {
  case class Props(heading: String, style: CommonStyle.Value = CommonStyle.default)

  val component = ReactComponentB[Props]("Panel")
    .render { (P, C) =>
      <.div(bss.panelOpt(P.style))(
        <.div(bss.panelHeading)(P.heading),
        <.div(bss.panelBody)(C)
      )
  }.build

  def apply(props: Props, children: ReactNode*) = component(props, children)
  def apply() = component
}
```

The panel provides no interactivity but this time we define a separate `heading` in addition to using the children property.

## Icons

Custom fonts are a great way to generate scalable icons that look good on all displays. In the tutorial we use
[Font Awesome](http://fortawesome.github.io/Font-Awesome/) icons and a simple wrapper that generates appropriate HTML tags to display the icon.

```scala
object Icon {
  type Icon = ReactTag
  def apply(name: String): Icon = <.i(^.className := s"fa fa-$name")

  def adjust = apply("adjust")
  def adn = apply("adn")
  .
  .
  def youtubePlay = apply("youtube-play")
  def youtubeSquare = apply("youtube-square")
}
```

## JavaScript chart component

If you'd want a nice charting component in your web UI you could go ahead and write a lot of SVG-generating code, but why bother when
there are so many components available for your benefit. Scala.js provides many ways to [use JavaScript](http://www.scala-js.org/doc/calling-javascript.html)
from your own Scala code and some of them are more type-safe than others. A good way is to define a *facade* for the 3rd party JS module and
for any data structures it may expose. This way you can be sure to use it in a type-safe manner.

In the tutorial we are using [Chart.js](http://www.chartjs.org/) but the same principles apply to practically all JS components out there.

The Chart.js draws the chart onto a HTML5 canvas and is instantiated by following JavaScript code

```javascript
var ctx = document.getElementById("myChart").getContext("2d");
var myNewChart = new Chart(ctx).Line(data);
```

To do the same in Scala.js we define a simple *facade* trait as follows

```scala
@JSName("Chart")
class JSChart(ctx: js.Dynamic) extends js.Object {
  def Line(data: ChartData): js.Dynamic = js.native
  def Bar(data: ChartData): js.Dynamic = js.native
}
```
Note that this defines only couple of charts available in the Chart.js component, but it's trivial to add more if you need them. We are also
skipping the `options` parameter for charts to keep things simple.

To actually instantiate the chart, we need access to the canvas element and with React this is a bit problematic since it builds a virtual-DOM and
updates the real DOM behind the scene. Therefore the canvas element does not exist at the time of `render` function call. To work around this problem
we need to build the chart in the `componentDidMount` function, which is called after the real DOM has been updated. This function is called with
a `scope` parameter that gives us access to the actual DOM node through `getDOMNode()`. The chart is built by creating a new instance of `Chart`
and calling the appropriate chart function.

```scala
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
```

Chart.js input data is a JavaScript object like below

```javascript
var data = {
    labels: ["January", "February", "March", "April", "May", "June", "July"],
    datasets: [
        {
            label: "My First dataset",
            fillColor: "rgba(220,220,220,0.2)",
            strokeColor: "rgba(220,220,220,1)",
            data: [65, 59, 80, 81, 56, 55, 40]
        },
        {
            label: "My Second dataset",
            fillColor: "rgba(151,187,205,0.2)",
            strokeColor: "rgba(151,187,205,1)",
            data: [28, 48, 40, 19, 86, 27, 90]
        }
    ]
};
```

To build the same in Scala.js we could directly use `js.Dynamic.literal` but that would be very unsafe and cumbersome. A better alternative is to define
a builder function to create it and a facade to access it.

```scala
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
```

In this case defining the `ChartData` trait is actually not necessary, since we don't really use it except to enforce type safety. But if you actually
need to access a JavaScript object defined outside your application, this is the way to do it. Defining chart data is now as simple as

```scala
val cp = ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
```

If you need to build/access very complex JavaScript objects, consider an option builder approach like the one in
[Querki](https://github.com/jducoeur/Querki/blob/master/querki/scalajs/src/main/scala/org/querki/jsext/JSOptionBuilder.scala) by
[jducoeur](https://github.com/jducoeur) (for example [JQueryUIDialog](https://github.com/jducoeur/Querki/blob/master/querki/scalajs/src/main/scala/org/querki/facades/jqueryui/JQueryUIDialog.scala)).

## Bootstrap jQuery components

Bootstrap is not only a CSS library but also comes with JavaScript to add functionality to components like Dropdown and Modal. The
[Modal](http://getbootstrap.com/javascript/#modals) is an especially problematic system as it involves a hidden dialog box that is shown when the modal is
activated and hidden afterwards. In a normal Bootstrap application you would define the dialog box HTML as part of your application and just kept it hidden.
With React, however, it's easy (and recommended) to create the HTML for the modal just before it's displayed, so that your application can easily control
the contents of the dialog box.

Before diving into the integration of the Bootstrap Modal, let's first examine how jQuery components can be integrated in general. We've provided a truly
[skeleton jQuery integration](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/components/JQuery.scala),
just enough for the modal to work, so you'll want to use something [more complete](https://github.com/jducoeur/jquery-facade) for
most purposes. The jQuery integration is also briefly explained in [Scala.js documentation](http://www.scala-js.org/doc/calling-javascript.html) so we won't go
into the details too much. Basically you need to define a global `jQuery` variable, through which you can then access the jQuery functionality. This is done
in the [`package.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/components/package.scala) for the
components package.

jQuery works by "calling" it with a selector or an element. In this tutorial we are always using a direct DOM element, so the facade only includes that option.
For example to attach an event listener to an element, you would call

```scala
jQuery(scope.getDOMNode()).on("hidden.bs.modal", null, null, scope.backend.hidden _)
```

jQuery has an extension mechanism where plugins can add new functions to the jQuery object. For example Bootstrap Modal adds a `modal` function. To define such
an extension in Scala.js we create a trait for it and an implicit conversion (just a type cast, really) for it.

```scala
trait BootstrapJQuery extends JQuery {
  def modal(action: String): BootstrapJQuery = js.native
  def modal(options: js.Any): BootstrapJQuery = js.native
}

implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]
```

Now whenever we want to call `jQuery(e).modal()` the compiler will automatically cast the `JQuery` type into `BootstrapJQuery`.

Armed with the jQuery integration we can now tackle the Modal itself. One of the problems the Modal poses is that it's dynamically shown and hidden by
the Bootstrap code and we need somehow control that. In this tutorial we've chosen a design where the modal doesn't even exist before it's needed and
it's shown right after it has been created. This leaves only the hiding part for us to handle.

In the `Backend` of the `Modal` we define a `hide()` function to do just that.

```scala
class Backend(t: BackendScope[Props, Unit]) {
  def hide(): Unit = {
    // instruct Bootstrap to hide the modal
    jQuery(t.getDOMNode()).modal("hide")
  }
```

However, because the dialog box itself contains controls that need to actually close the dialog, we need to expose this functionality to the parent
component via properties.

```scala
// header and footer are functions, so that they can get access to the the hide() function for their buttons
case class Props(header: (Backend) => ReactNode, footer: (Backend) => ReactNode, closed: () => Unit, backdrop: Boolean = true,
                 keyboard: Boolean = true)
```

Additionally, the Bootstrap modals are faded in and out, so the parent cannot go ahead and remove the modal HTML from DOM right away, but
it needs to wait for the fade-out to complete. This is accomplished by listening to an event and calling the parent's `closed` function afterwards.
```scala
// jQuery event handler to be fired when the modal has been hidden
def hidden(e: JQueryEventObject): js.Any = {
  // inform the owner of the component that the modal was closed/hidden
  t.props.closed()
}
...
// register event listener to be notified when the modal is closed
jQuery(scope.getDOMNode()).on("hidden.bs.modal", null, null, scope.backend.hidden _)
```

To show the dialog box after it has been created, we again call `modal()` via jQuery in `componentDidMount`.
```scala
.componentDidMount(scope => {
  val P = scope.props
  // instruct Bootstrap to show the modal
  jQuery(scope.getDOMNode()).modal(js.Dynamic.literal("backdrop" -> P.backdrop, "keyboard" -> P.keyboard, "show" -> true))
```

