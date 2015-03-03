# Scala.js SPA-tutorial

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ochrons/scalajs-spa-tutorial?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Tutorial for creating a simple Single Page Application with [Scala.js](http://www.scala-js.org/) and [Spray](http://spray.io/).

- [Purpose](#purpose)
- [Getting started](#getting-started)
- [Application structure](#application-structure)
    - [The Client](#the-client)
        - [Routing](#routing)
        - [Main menu](#main-menu)
        - [Dashboard](#dashboard)
            - [Message of the day](#message-of-the-day)
            - [Links to other routes](#links-to-other-routes)
        - [Integrating JavaScript components](#integrating-javascript-components)
            - [Bootstrap CSS components](#bootstrap-css-components)
            - [Icons](#icons)
            - [JavaScript chart component](#javascript-chart-component)
            - [Bootstrap jQuery components](#bootstrap-jquery-components)
        - [Todo module and data flow](#todo-module-and-data-flow)
            - [Unidirectional data flow](#unidirectional-data-flow)
            - [Modifying a Todo state](#modifying-a-todo-state)
            - [Wiring](#wiring)
            - [Editing todos](#editing-todos)
        - [Autowire and uPickle](#autowire-and-upickle)
    - [Server side](#server-side)
- [Testing](#testing)
- [SBT build definition](#sbt-build-definition)
    - [Serving compiled JS from both projects](#serving-compiled-js-from-both-projects)
    - [Sharing web resources between JS and JVM projects](#sharing-web-resources-between-js-and-jvm-projects)
    - [Using resources from WebJars](#using-resources-from-webjars)
        - [WebJar JavaScript](#webjar-javascript)
        - [WebJar CSS/LESS](#webjar-cssless)
        - [WebJar resource files](#webjar-resource-files)
    - [Running the server in a separate JVM](#running-the-server-in-a-separate-jvm)
    - [Production build](#production-build)
        - [Configuring between development and production](#configuring-between-development-and-production)
        - [Using optimized versions of JS libraries](#using-optimized-versions-of-js-libraries)
        - [Packaging an application](#packaging-an-application)
        - [Automating the release build](#automating-the-release-build)
- [FAQ](#faq)
- [What next?](#what-next)

## Purpose

This project demonstrates typical design patterns and practices for developing SPAs with Scala.js with special focus on building a complete application.
It started as a way to learn more about Scala.js and related libraries, but then I decided to make it more tutorial-like for the greater good :)

The code covers typical aspects of building a SPA using Scala.js but it doesn't try to be an all-encompassing example for all the things possible with Scala.js.
Before going through this tutorial, it would be helpful if you already know the basics of Scala.js and have read through the official
[Scala.js tutorial](http://www.scala-js.org/doc/tutorial.html) and the great e-book [Hands-on Scala.js](http://lihaoyi.github.io/hands-on-scala-js/#Hands-onScala.js)
by [Li Haoyi (lihaoyi)](https://github.com/lihaoyi).

# Getting started

Fork a copy of the repository and clone it to your computer using Git. Run `sbt` in the project folder and after SBT has completed loading the project,
start the server with `re-start`. This will compile both the client and server side Scala application, package it and start the server. You can now navigate to
`localhost:8080` on your web browser to open the Dashboard view. It should look something like this

![dashboard](/../screenshots/screenshots/dashboard.PNG?raw=true)

The application is really simple, containing only two views (Dashboard and Todo) and you can access these by clicking the appropriate item on the menu. The Todo
view looks like this

![todos](/../screenshots/screenshots/todos.png?raw=true)

Now that you have everything up and running, it's time to dive into the details of what makes this application tick. Or if you want to experiment a little
yourself, use the `~fastOptJS` command on SBT prompt and SBT will automatically compile the (client side) application when you modify the source code. Try
changing for example the chart data in `js/Dashboard.scala` and reloading the web page.

# Application structure

The application is divided into three folders: `js`, `jvm` and `shared`. As the names imply, `js` contains the client code for the SPA, `jvm` is the server and
`shared` contains code and resources used by both. If you take a quick look at [`project/build.scala`](project/build.scala) you will notice the use of
`crossProject` to define this Scala.js specific [cross-building](http://www.scala-js.org/doc/sbt/cross-building.html) project structure.

Within each sub-project the usual SBT/Scala directory structure convention is followed.

We'll get to the details of the project build file later on, but let's first take a look at actual client code!

## The Client

As is typical for SPAs the client consists of a single HTML file and a number of supporting resources (JS and CSS). One of these resources is the actual JavaScript
code generated by Scala.js from the Scala sources. There are two variants of the `index.html` one for fast development and the other (`index-full.html`) for
a production optimized version. Both are stored under `shared` project so that you can access them not only through the server, but also when doing local
development with the JS client only (more about this later). All the relevant resources are under the `web` directory to prevent conflicts with other (server)
resources.

In the `index.html` you'll find the usual HTML things like links to CSS and JS files. As you can see, the `<body>` element is pretty empty, because all the
HTML will be generated by the application itself.

```html
<body onload="SPAMain().main()">
<script src="js/scalajs-spa-jsdeps.js"></script>
<script src="js/scalajs-spa-fastopt.js"></script>
</body>
</html>
```

Instead of using external JavaScript references to [React](http://facebook.github.io/react/), [jQuery](http://jquery.com/), [Bootstrap](http://getbootstrap.com/) and
to a [chart component](http://www.chartjs.org/), the build system combines all these into a single JavaScript file (scalajs-spa-jsdeps.js). See [here](#webjar-javascript)
for details. The last JavaScript reference is the compiled application code.

Once the browser has loaded all the resources, it will call the `SPAMain().main()` method defined in the
[`SPAMain.scala`](js/src/main/scala/spatutorial/client/SPAMain.scala) singleton class. This is the entry point of the application. The class itself is very simple,

```scala
@JSExport("SPAMain")
object SPAMain extends JSApp {
  @JSExport
  def main(): Unit = {
    // build a baseUrl, this method works for both local and server addresses (assuming you use #)
    val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
    val router = MainRouter.router(baseUrl)

    // tell React to render the router in the document body
    React.render(router(), dom.document.body)
  }
}
```

The externally accessible classes and functions are annotated with `@JSExport` so the Scala.js compiler knows not to optimize them away and make them available
with those exact names in the global scope.

What `main()` does is simply to create a *router* and instruct React to render it inside the document `<body>` tag.

Now at this point you've seen couple of references to React and might wonder what's it about. [React](http://facebook.github.io/react/) is a JavaScript
library for building user interfaces, developed by Facebook. You might ask "why use a JavaScript library with Scala.js if Scala is so great" and yes,
it might make sense to do a similar library in Scala.js but since it's already there, why not use it. And to sweeten the deal there is a very nice
wrapper for React called [scalajs-react](https://github.com/japgolly/scalajs-react) by [David Barri (japgolly)](https://github.com/japgolly).

There are also other Scals.js libraries available for building SPAs but I wanted to go with React, so that's what we'll use in this tutorial :)

### Routing

A critical feature in a SPA is navigation between "pages" within the application. Of course they are not real pages, since it's a *Single Page* Application,
but from the user point of view it looks like that. A typical example of a SPA is Gmail where the URL in the browser reflects the state of the application.

Since we are not loading new pages from the server, we cannot use the regular browser navigation but need to provide one ourselves. This is called *routing* and
is provided by many JS frameworks like AngularJS. Scala.js itself is not an application framework so there is no ready made router component provided by it. But
we are lucky to have developers like @japgolly who go through all the pain and suffering to deliver great libraries for the rest of us. In the tutorial I'm using
[`scalajs-react` router](https://github.com/japgolly/scalajs-react/blob/master/extra/ROUTER.md) which nicely integrates with `scalajs-react` and provides a
seamless way to manage routes and navigate between them.

The way it works is that you basically create route components, register them with the router and off it goes binding your components as the URL changes. The
provided examples build all routes within a single class, but in real life we want more modularity. In this tutorial we have a total of *two* modules/routes/views
just to demonstrate how to use the router.

As this is a Single Page Application, all routes are defined under one router, `MainRouter`, which extends `RoutingRules` trait.

```scala
object MainRouter extends RoutingRules {
  // register the components and store locations
  val dashboardLoc = register(rootLocation(Dashboard.component))
  val todoLoc = register(location("#todo", ToDo.component))
```

Here we just register the two routes with the `RoutingRules`. First route is our main route which is attached to the special `rootLocation`. The second
route is attached to `#todo` path. You could also use "clean" paths without the hash, but then your server must be prepared to server correct content
even when there is a sub-path defined. The hash also makes it easy to work with a local-only setup, when the files are served by the Workbench plugin.

The `MainRouter` also provides the base HTML code and integrates a `MainMenu` component for the application in its `interceptRender` function. SPA tutorial
uses Bootstrap CSS to provide a nice looking layout, but you can use whatever CSS framework you wish just by changing the CSS class definitions.

```scala
import japgolly.scalajs.react.vdom.prefix_<^._

override protected def interceptRender(ic: InterceptionR) = {
  <.div(
    <.nav(^.className := "navbar navbar-inverse navbar-fixed-top")(
      <.div(^.className := "container")(
        <.div(^.className := "navbar-header")(<.span(^.className := "navbar-brand")("SPA Tutorial")),
        <.div(^.className := "collapse navbar-collapse")(
          MainMenu(MainMenu.Props(ic.loc, ic.router, TodoStore.todos))
        )
      )
    ),
    // currently active module is shown in this container
    <.div(^.className := "container")(ic.element)
  )
}
```

See how the code looks just like HTML, except it's type safe and the IDE provides auto-complete! If you insist on having even closer resemblance to HTML,
you can replace the `prefix_<^` with `all` giving you simple `div` and `className` tag names. Be warned, however, that this may lead to nasty surprises
down the road because the HTML namespace contains a lot of short, common tag names like `a` and `id`. The little extra effort from `<.` and `^.` pays
off in the long run.

### Main menu

The main menu is just another React component that is given the current location and the router as properties. The contents of the menu is defined
statically within the class itself, because the referred locations are anyway all known at compile time. For other kinds of menus you'd want to use
a dynamic system, but static is just fine here.

```scala
case class MenuItem(label: (Props) => ReactNode, icon: Icon, location: MainRouter.Loc)

private val menuItems = Seq(
  MenuItem(_ => "Dashboard", Icon.dashboard, MainRouter.dashboardLoc),
  MenuItem(buildTodoMenu, Icon.check, MainRouter.todoLoc)
)

private def buildTodoMenu(props: MenuProps): ReactNode = {
  val todoCount = props.todos().count(!_.completed)
  Seq(
    <.span("Todo "),
    if (todoCount > 0) <.span(^.className := "label label-danger label-as-badge", todoCount) else <.span()
  )
}
```

For each menu item we define a function to generate the label, an icon and the location that was registered in the `MainRouter`. For Dashboard
the label is simple text, but for Todo we also render the number of open todos.

To render the menu we just loop over the items and create appropriate tags. For links we need to use the `router` provided in the properties.

```scala
val MainMenu = ReactComponentB[MenuProps]("MainMenu")
  .render(P => {
  <.ul(^.className := "nav navbar-nav")(
    // build a list of menu items
    for (item <- menuItems) yield {
      <.li((P.activeLocation == item.location) ?= (^.className := "active"),
        P.router.link(item.location)(item.icon, " ", item.label(P))
      )
    }
  )
})
  .build
```

Ok, we've got the HTML page defined, menu generated and the active component (Dashboard) within the placeholder, what happens next?

### Dashboard

[Dashboard module](js/src/main/scala/spatutorial/client/modules/Dashboard.scala) is really simple in terms of React components as it contains
no internal state nor backend functionality. It's basically just a placeholder for two other components `Motd` and `Chart`. The only method is
the `render` method which is responsible for rendering the component when it's mounted by React. It also provides fake data for the Chart
component, to keep simple.

```scala
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
```

#### Message of the day

[Motd component](js/src/main/scala/spatutorial/client/components/Motd.scala) is a simple React component that fetches *Message of the day*
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

How the magic of calling the server actually happens is covered in a [later chapter](#autowire-and-upickle).

#### Links to other routes

Sometimes you need to create a link that takes the user to an another module behind a route. To create these links in a type-safe manner,
the tutorial code defines a specific trait with functions that return valid links.

```scala
trait AppLinks {
  def dashboard(content: TagMod*): ReactTag
  def todo(content: TagMod*): ReactTag
}
```

This `AppLinks` trait can be passed down from the top-level modules to those components that need to create links. To get an instance of the
`AppLinks` you need to call `MainRouter.appLinks(router)`. It requires the router as a parameter, which is only available in the internal
methods like `render` of the top-level modules.

The content of the link can be anything from a simple text string to a complex VDOM structure.

### Integrating JavaScript components

Although Scala.js provides a superb environment for developing web clients, sometimes it makes sense to utilize the hard work of
thousands of JavaScript developers fumbling in the shadows :)

#### Bootstrap CSS components

[Bootstrap](http://getbootstrap.com/) is a popular HTML/CSS/JS framework by Twitter for developing responsive applications. It comes with a lot
of styled HTML/CSS components that are easy to use and integrate into your application. Lot of Bootstrap actually doesn't even use JavaScript, all
the magic happens in CSS.

This tutorial wraps couple of simple Bootstrap [components](js/src/main/scala/spatutorial/client/components/Bootstrap.scala)
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
    <.button(^.className := s"btn btn-${P.style}", ^.tpe := "button", ^.onClick --> P.onClick())(C)
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
    <.div(^.className := s"panel panel-${P.style}")(
      <.div(^.className := "panel-heading")(P.heading),
      <.div(^.className := "panel-body")(C)
    )
  }.build

  def apply(props: Props, children: ReactNode*) = component(props, children)
  def apply() = component
}
```

The panel provides no interactivity but this time we define a separate `heading` in addition to using the children property.

#### Icons

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

#### JavaScript chart component

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

#### Bootstrap jQuery components

Bootstrap is not only a CSS library but also comes with JavaScript to add functionality to components like Dropdown and Modal. The
[Modal](http://getbootstrap.com/javascript/#modals) is an especially problematic system as it involves a hidden dialog box that is shown when the modal is
activated and hidden afterwards. In a normal Bootstrap application you would define the dialog box HTML as part of your application and just kept it hidden.
With React, however, it's easy (and recommended) to create the HTML for the modal just before it's displayed, so that your application can easily control
the contents of the dialog box.

Before diving into the integration of the Bootstrap Modal, let's first examine how jQuery components can be integrated in general. We've provided a truly
[skeleton jQuery integration]((https://github.com/ochrons/scalajs-spa-tutorial/tree/master/js/src/main/scala/spatutorial/client/components/JQuery.scala)),
just enough for the modal to work, so you'll want to use something [more complete](https://github.com/scala-js/scala-js-jquery) for
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

### Todo module and data flow

The Todo module and its React component are a bit more interesting than Dashboard as they provide some interaction. The module contains a `TodoList`
component, displaying a list of Todo items retrieved from the server. User can then click the checkbox next to the item to indicate if that item
is completed or not. Internally the state of Todo items is maintained by the `Todo` module and `TodoList` just passively displays them.

Before going into the details of the actual Todo module and related components, let's ponder a while about data flow in a Scala.js React application.

#### Unidirectional data flow

Several JS frameworks out there (like AngularJS) use mutable state and two-way data binding. In this tutorial, however, we are taking
cues from Facebook's [Flux](https://github.com/facebook/flux), which is an architecture for unidirectional data flow and immutable state. 
This architecture works especially well in more complex applications, where two-way data binding can quickly lead to all kinds of hard issues. 
It's also a relatively simple concept, so it works well even in a simple tutorial application like this. Below you can see a diagram of the
Flux architecture.

![Flux architecture](http://facebook.github.io/flux/img/flux-simple-f8-diagram-with-client-action-1300w.png)

It consists of a Dispatcher that takes in *Actions*, and dispatches them to all *Stores* that then inform
*Views* to update themselves with the new data. This kind of message dispatching sounds quite familiar if you've used actor frameworks
like [Akka](http://akka.io) before, so we might as well call those parts with a bit different names.

![Actor architecture](/../screenshots/screenshots/dispatcher-actor.png?raw=true)

It's not a real actor framework, for example the dispatcher sends all messages to all registered actors, but it's close enough for our
purposes. To explain how this works in practice, let's look at the concrete examples in the Todo module.

#### Modifying a Todo state 

The `TodoList` component renders a checkbox for each Todo, which can be used to mark the Todo as *completed*.

```scala
<.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
```

Clicking the checkbox calls the `stateChange` method, which maps to `updateTodo` method in `TodoActions` object. The `TodoActions` is a
collection of functions to communicate with the server about changes to the todo list.

```scala
object TodoActions {
  def updateTodo(item: TodoItem) = {
    // inform the server to update/add the item
    AjaxClient[Api].updateTodo(item).call().foreach { todos =>
      MainDispatcher.dispatch(UpdateAllTodos(todos))
    }
  }
```

Note that nothing really happens on the client side as a result of clicking the checkbox. All it does is tell the server that the state
of completion has changed on the selected Todo. Only after the call to the server returns with an updated list of todos, is the client updated.
The update happens indirectly by sending an `UpdateAllTodos` message to the `MainDispatcher`. This message is then dispatched to all registered
actors, which in this case means only the `TodoStore`.

```scala
// refine a reactive variable
private val items = Var(Seq.empty[TodoItem])

private def updateItems(newItems: Seq[TodoItem]): Unit = {
  // check if todos have really changed
  if (newItems != items()) {
    // use Rx to update, which propagates down to dependencies
    items() = newItems
  }
}

override def receive = {
  case UpdateAllTodos(todos) =>
    updateItems(todos)
```

`TodoStore` updates its internal state with the new items (only if there is a real change) and this change is propagated using 
[ScalaRx](https://github.com/lihaoyi/scala.rx) to all interested parties. As it happens, there are
actually two separate components observing changes in the `TodoStore`. One is the Todo module where we started:

```scala
// get todos from the Rx defined in props
TodoList(TodoListProps(P.todos(), TodoActions.updateTodo, item => B.editTodo(Some(item)), B.deleteTodo))
```

It forces an update on the component, which in turn causes a call to `render` to refresh the view. Within the view, the new todos are fetched from
`TodoStore` using a reactive variable (`Rx`). This change cascades down to `TodoList` and to the individual Todo that was originally clicked. 
Mission accomplished!

But as we mentioned before, there was another component interested in changes to the Todos. This is the main menu item for Todo, which shows
the number of open Todos.

```scala
val todoCount = props.todos().count(!_.completed)
Seq(
  <.span("Todo "),
  if (todoCount > 0) <.span(^.className := "label label-danger label-as-badge", todoCount) else <.span()
)
```

This is the beauty of unidirectional data flow, where the components do not need to know where the change came from, or who might be
interested in the change. All state changes are propagated to interested parties automatically.

Next, let's look how to set up everything for data to flow.

#### Wiring

![Wiring](/../screenshots/screenshots/control-flow.png?raw=true)

As you can see in the diagram above, there's all kinds of control flow activity going on in the application. Relevant classes are:

**MainDispatcher**
* Singleton instance of `Dispatcher` that everything is using

**TodoStore**
* A store implementing both `Actor` for receiving messages from `Dispatcher`. Provides reactive variables (`Rx`) to propagate changes.

**TodoActions**
* Helper class to make Ajax calls to the server and initiate updates via the dispatcher

`TodoStore` registers itself with the `MainDispatcher` in its singleton constructor.
```scala
MainDispatcher.register(this)
```

The `Todo` module and `MainMenu` register themselves, when they are mounted, as observers to changes in `TodoStore` items. 
They also initiate a refresh request for the todos.

```scala
def mounted(): Unit = {
  // hook up to TodoStore changes
  val obsTodos = t.props.todos.foreach { _ => t.forceUpdate() }
  onUnmount {
    // stop observing when unmounted
    obsTodos.kill()
  }
  // dispatch a message to refresh the todos, which will cause TodoStore to fetch todos from the server
  MainDispatcher.dispatch(RefreshTodos)
}
```
The `foreach` call returns an `Obs` which we can use to observe changes. Whenever `todos` change, the function body is executed, forcing an update on
the React component. The actual value of `todos` is not used here, but within the view definition.
 
Note the use of [onUnmount](https://github.com/japgolly/scalajs-react/tree/master/extra#onunmount) to automatically remove the observer
when the component is unmounted.

Another, cleaner way to do the same is to use an observer pattern.

```scala
abstract class RxObserver[BS <: BackendScope[_,_]](scope: BS) extends OnUnmount {
  protected def observe[T](rx:Rx[T]): Unit = {
    val obs = rx.foreach( _ => scope.forceUpdate() )
    // stop observing when unmounted
    onUnmount(obs.kill())
  }
}

class Backend(t: BackendScope[Props, State]) extends RxObserver(t) {
  def mounted(): Unit = {
    // hook up to TodoStore changes
    observe(t.props.todos)
  }
```

This does the same thing as before but a bit more elegantly. The `RxObserver` trait is independent and can be used for all Backend classes that
have dependencies on reactive variables.

Finally views hook up different callbacks to `TodoActions` when something happens to the todos.

#### Editing todos

For adding new Todo items, the user interface provides a button and a modal dialog box (using the `Modal` component described earlier). Editing
an existing item is performed by clicking an *Edit* button next to the todo description. Both actions open the same dialog. Finally you can also
delete a todo by clicking the *Delete* button.

`TodoForm` is a simple React component built around a `Modal` which allows users to edit an existing Todo item, or create a new one 
(there is no difference between these two from the component's point of view). It looks like this

![Dialog box](/../screenshots/screenshots/dialogbox.png?raw=true)

The dialog gets an optional item in properties and maintains current item in its state. The `submitHandler` callback is used to inform the parent
when the dialog box is closed (or cancelled).

```scala
case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) => Unit)

case class State(item: TodoItem, cancelled: Boolean = true)
```

Building the component looks a bit complicated, so let's walk through it.
```scala
val component = ReactComponentB[Props]("TodoForm")
  .initialStateP(p => State(p.item.getOrElse(TodoItem("", "", TodoNormal, false))))
  .backend(new Backend(_))
  .render((P, S, B) => {
    val headerText = if (S.item.id == "") "Add new todo" else "Edit todo"
    Modal(Modal.Props(
      // header contains a cancel button (X)
      header = be => <.span(<.button(^.tpe := "button", ^.className := "close", ^.onClick --> be.hide(), Icon.close), <.h4(headerText)),
      // footer has the OK button that submits the form before hiding it
      footer = be => <.span(Button(Button.Props(() => {B.submitForm(); be.hide()}), "OK")),
      // this is called after the modal has been hidden (animation is completed)
      closed = B.formClosed),
      <.div(^.className := "form-group",
        <.label(^.`for` := "description", "Description"),
        <.input(^.tpe := "text", ^.className := "form-control", ^.id := "description", ^.value := S.item.content,
          ^.placeholder := "write description", ^.onChange ==> B.updateDescription)),
      <.div(^.className := "form-group",
        <.label(^.`for` := "priority", "Priority"),
        // using defaultValue = "Normal" instead of option/selected due to React
        <.select(^.className := "form-control", ^.id := "priority", ^.value := S.item.priority.toString, ^.onChange ==> B.updatePriority,
          <.option(^.value := TodoHigh.toString, "High"),
          <.option(^.value := TodoNormal.toString, "Normal"),
          <.option(^.value := TodoLow.toString, "Low")
        )
      )
    )
  }).build
```

State is first initialized with the provided item or with a new empty item. Within the `render` method a new `Modal` is created and in the properties we assign
couple of button controls. Note how both `header` and `footer` are actually functions that are given the `Modal`'s `Backend` so that they can call the `hide()`
function. In the OK button the state is first updated to _not cancelled_.

The form itself is quite straightforward, with handlers to update internal state as fields change. Note that with React the `select` element works a bit
differently from regular HTML5 and you must use `value` property to select the option instead of the typical `selected` attribute.

When the form closes, the parent's `submitHandler` gets called with the item and a flag indicating if the dialog box was cancelled.
```scala
def formClosed(): Unit = {
  // call parent handler with the new item and whether form was OK or cancelled
  t.props.submitHandler(t.state.item, t.state.cancelled)
}
```

But now it's time to get to the bottom of client-server communications!

### Autowire and uPickle

Web clients communicate with the server most commonly with *Ajax* which is quite loosely defined collection of techniques. Most notable
JavaScript libraries like JQuery provide higher level access to the low level protocols exposed by the browser. Scala.js provides a nice
Ajax wrapper in `dom.extensions.Ajax` (or `dom.ext.Ajax` in scalajs-dom 0.8+) but it's still quite tedious to serialize/deserialize objects
and take care of all the dirty little details.

But fear not, there is no need to do all that yourself as our friend [Li Haoyi (lihaoyi)](https://github.com/lihaoyi) has created and
published two great libraries called [uPickle](https://github.com/lihaoyi/upickle) and [Autowire](https://github.com/lihaoyi/autowire).

To build your own client-server communication pathway all you need to do is to define a single object on the client side and another on the
server side.

```scala
// client side
object AjaxClient extends autowire.Client[String, upickle.Reader, upickle.Writer]{
  override def doCall(req: Request): Future[String] = {
    dom.extensions.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = upickle.write(req.args)
    ).map(_.responseText)
  }

  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
```

The only variable specific to your application is the URL you want to use to call the server. Otherwise everything else it automatically
generated for you through the magic of macros. The server side is even simpler, just letting Autowire know that you want to use uPickle
for serialization.

```scala
// server side
object Router extends autowire.Server[String, upickle.Reader, upickle.Writer]{
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
```

Now that you have the `AjaxClient` set up, calling server is as simple as

```scala
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._

AjaxClient[Api].getTodos().call().foreach { todos =>
  println(s"Got some things to do $todos")
}
```

Note that you need those two imports to access the Autowire magic and to provide an execution context for the futures.

The `Api` is just a simple trait shared between the client and server.

```scala
trait Api {
  // message of the day
  def motd(name:String) : String

  // get Todo items
  def getTodos() : Seq[TodoItem]

  // update a Todo
  def updateTodo(item:TodoItem)
}
```

Please check out uPickle documentation on what it can and cannot serialize. You might need to use something else if your data is complicated.
Case classes, base collections and basic data types are a safe bet.

So how does this work on the server side?

## Server side

The tutorial server is very simplistic and does not represent a typical Spray application, but it's enough to provide some basic support
for the client side. Routing logic on the server side is defined using Spray DSL

```scala
startServer("0.0.0.0", port = port) {
  get {
    pathSingleSlash {
      // serve the main page
      if(Config.productionMode)
        getFromResource("web/index-full.html")
      else
        getFromResource("web/index.html")
    } ~
      // serve other requests directly from the resource directory
      getFromResourceDirectory("web")
  } ~ post {
    path("api" / Segments) { s =>
      extract(_.request.entity.asString) { e =>
        complete {
          // handle API requests via autowire
          Router.route[Api](apiService)(
            autowire.Core.Request(s, upickle.read[Map[String, String]](e))
          )
        }
      }
    }
  }
}
```

The main HTML page and related resources are provided directly from the project resources directory (coming from the `shared` sub-project, actually).
The interesting part is handling `api` requests using Autowire router. Like on the client side, Autowire takes care of the complicated stuff
so you just need to plug it in and let it do its magic. The `ApiService` is just a normal class and it doesn't need to concern itself with
serialization or URL request path mappings. It just implements the same `Api` as it used on the client side. Simple, eh!

```scala
class ApiService extends Api {
  var todos = Seq(
    TodoItem("1", "Wear shirt that says 'Life'. Hand out lemons on street corner.", TodoLow, false),
    TodoItem("2", "Make vanilla pudding. Put in mayo jar. Eat in public.", TodoNormal, false),
    TodoItem("3", "Walk away slowly from an explosion without looking back.", TodoHigh, false),
    TodoItem("4", "Sneeze in front of the pope. Get blessed.", TodoNormal, true)
  )

  override def motd(name: String): String = s"Welcome to SPA, $name! Time is now ${new Date}"

  override def getTodos(): Seq[TodoItem] = {
    // provide some fake Todos
    todos
  }

  // update a Todo
  override def updateTodo(item: TodoItem): Unit = {
    // TODO, update database etc :)
    println(s"Todo item was updated: $item")
    if(todos.exists(_.id == item.id)) {
      todos = todos.collect {
        case i if i.id == item.id => item
        case i => i
      }
    } else {
      // add a new item
      todos :+= item.copy(id = UUID.randomUUID().toString)
    }
  }
}
```

# Testing

Testing Scala.js application is as easy as testing regular Scala applications, except you have to choose a test framework that is OK with
the limitations of the JavaScript environment. Many popular frameworks like ScalaTest and Specs2 depend on JVM features (like reflection) that
are not available in the JS land, so Li Haoyi went ahead and created [uTest](https://github.com/lihaoyi/utest), a simple yet powerful
testing framework that works wonderfully well with Scala.js.

To define tests, you just need to extend from `TestSuite` and override the `tests` method.

```scala
object DispatcherTests extends TestSuite {
  override def tests = TestSuite {
    'test { ... }
  }
}
```

Take a look at [`DispatcherTests.scala`](js/src/test/scala/spatutorial/client/ukko/DispatcherTests.scala) for some examples of test cases.

To run tests in SBT, you'll need to add a dependency for `"com.lihaoyi" % "utest" % "0.3.0"` and configure the test framework with
`testFrameworks += new TestFramework("utest.runner.Framework")`. Now you can run the tests using regular `test` and `testOnly` commands
in the SBT prompt.

# SBT build definition

Since Scala.js is quite new and it's been evolving even rather recently, building Scala.js applications with SBT is not as clear as it could
be. Yes, the documentation and tutorials give you the basics, but what if you want something more, like put the output of the Scala.js compiler
into another directory?

The `build.scala` in this tutorial shows you some typical cases you might run into in your own application.

## Serving compiled JS from both projects

Scala.js SBT [Workbench plugin](https://github.com/lihaoyi/workbench) enables quick development by serving client files straight from SBT
without the need for your own server. The relevant part in `build.scala` is

```scala
// define where the JS-only application will be hosted by the Workbench plugin
localUrl :=("localhost", 13131),
refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile),
bootSnippet := "SPAMain().main();"
```

To use the Workbench plugin, point your browser to http://localhost:13131/jvm/target/scala-2.11/classes/web/index.html. Whenever you run `fastOptJS` the plugin will automatically refresh the browser with an updated version. How's that for fast turn-around times!

But since Scala.js compile output is stored under the JS project by default, how can we serve it from the JVM project? The solution is to
instruct Scala.js to save its output in a specific directory under the JVM project and then make a copy back to the JS side. For this you need
a bit more SBT code to make it work

```scala
// configure a specific directory for scalajs output
val scalajsOutputDir = Def.settingKey[File]("directory for javascript files output by scalajs")

// make all JS builds use the output dir defined later
lazy val js2jvmSettings = Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
  crossTarget in(spaJS, Compile, packageJSKey) := scalajsOutputDir.value
}

// instantiate the JS project for SBT with some additional settings
lazy val spaJS: Project = spa.js.settings(
  fastOptJS in Compile := {
    // make a copy of the produced JS-file (and source maps) under the spaJS project as well,
    // because the original goes under the spaJVM project
    // NOTE: this is only done for fastOptJS, not for fullOptJS
    val base = (fastOptJS in Compile).value
    IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / base.data.getName)
    IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / (base.data.getName + ".map"))
    base
  }
)

// instantiate the JVM project for SBT with some additional settings
lazy val spaJVM: Project = spa.jvm.settings(js2jvmSettings: _*).settings(
  // scala.js output is directed under "web/js" dir in the spaJVM project
  scalajsOutputDir := (classDirectory in Compile).value / "web" / "js",
  // reStart depends on running fastOptJS on the JS project
  Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in(js, Compile))
)
```

## Sharing web resources between JS and JVM projects

In addition to the compiled Scala.js code, you need other resources such as the `index.html` some CSS and JS files for your application to work.
You should store these under the `shared` project resources and let the JS/JVM project know about these extra resource directories.

```scala
val sharedSrcDir = "shared"

// copy resources from the "shared" project
unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",
```

## Using resources from WebJars

[WebJars](http://www.webjars.org) are a wonderful way to simplify inclusion of external resources such as JavaScript libraries and CSS definitions into
your own project. Instead of downloading JS/CSS packages like Bootstrap and extracting them within your project (or referring to external CDN
served resources), you can just add a dependency to the appropriate WebJar and you're all set! Well, except you aren't, unless you were using something
like Play, which this tutorial isn't using. So how do you actually get those resources out of a WebJar and into your web resource directory? The process
is a bit different for JavaScript, CSS (LESS) and other resources files.

### WebJar JavaScript

Scala.js SBT plugin offers a [nice and convenient way](http://www.scala-js.org/doc/sbt/depending.html) for extracting JavaScript sources from various
WebJars and concatenating them into a single JavaScript file that you can then refer to in your `index.html`. In the tutorial project this means following
configuration in the `build.scala` file:

```scala
/** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
val jsDependencies = Def.setting(Seq(
  "org.webjars"  % "react"      % "0.12.1"  / "react-with-addons.js"  commonJSName "React",
  "org.webjars"  % "jquery"     % "1.11.1"  / "jquery.js",
  "org.webjars"  % "bootstrap"  % "3.3.2"   / "bootstrap.js"          dependsOn    "jquery.js",
  "org.webjars"  % "chartjs"    % "1.0.1"   / "Chart.js"
))

.jsSettings(
      jsDependencies      ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value},
      jsDependencies      +=  RuntimeDOM % "test",
      skip in packageJSDependencies := false,
```

This will produce a file named `web/js/scalajs-spa-jsdeps.js` containing all those JavaScript files combined.

### WebJar CSS/LESS

For extracting CSS files from WebJars you could use the method described below, but there is bit more convenient method that gives you [LESS](http://lesscss.org/)
processing as a bonus. First we'll need to add the [sbt-less](https://github.com/sbt/sbt-less) plugin into our `plugins.sbt`

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
```

The projects also need to enable the `sbt-web` plugin for this to work.

```scala
jvm.enablePlugins(SbtWeb)
js.enablePlugins(SbtWeb)
```

Because `sbt-less` and other `sbt-web` plugins are heavily related to the Play framework, we'll need to do some additional configuration to make it work with
our simple tutorial. We'll be storing LESS files under `src/main/assets` to keep them separated from directly copied resources.

```scala
import com.typesafe.sbt.less.Import._

sourceDirectory in Assets    := baseDirectory.value / "src" / "main" / "assets",
LessKeys.compress in Assets  := true,
```
The last line tells the LESS compiler to minify the produced CSS.

Next step is to create a `main.less` (yes, it has to be named exactly that) with references to CSS/LESS files inside the WebJars.
Note that due to a [bug](https://github.com/sbt/sbt-less/issues/30) in `sbt-web` we cannot put the `mail.less` under the `shared` project where it belongs, but
must duplicate it to both JS and JVM projects.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";

body {
    padding-top: 50px;
}

.label-as-badge {
    border-radius: 1em;
}
```

In this case we import Bootstrap and Font Awesome LESS files. Depending on the WebJar, it may or may not contain LESS files in addition to the CSS file. With
the LESS files you can easily [configure the library](http://getbootstrap.com/css/#less) to your liking by defining CSS variables in your `main.less` file.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
@brand-danger:  #00534f;
```

The problem with the `sbt-less` plugin is that it saves the compiled CSS file into wrong directory (since we are not using Play), so we need to copy it
to the correct location. This will be covered in the next section.

### WebJar resource files

Sometimes WebJars contain other useful resources, such as the font files for Font Awesome in our case. Just including the WebJar as a dependency will provide
us the extracted contents, but it's in the wrong place and doesn't get bundled into the final package. So we will need to copy relevant files where
we want them.

To achieve this, we'll define our very own SBT task to do the job.
```scala
val copyWebJarResources = TaskKey[Unit]("Copy resources from WebJars")

copyWebJarResources := {
  // copy the compiled CSS
  val s = streams.value
  s.log("Copying webjar resources")
  val compiledCss = webTarget.value / "less" / "main" / "stylesheets"
  val targetDir = (classDirectory in Compile).value / "web"
  IO.createDirectory(targetDir / "stylesheets")
  IO.copyDirectory(compiledCss, targetDir / "stylesheets")
  // copy font-awesome fonts from WebJar
  val fonts = (webModuleDirectory in Assets).value / "webjars" / "lib" / "font-awesome" / "fonts"
  IO.createDirectory(targetDir / "fonts")
  IO.copyDirectory(fonts, targetDir / "fonts")
}
```

Without going into the SBT details, this task copies the generated CSS file (and its sourcemap) into `web/stylesheets`. It also copies all files found
under the Font Awesome `fonts` folder into the appropriate place under our project. If you need to copy other resources, you can use the same code just by
modifying the path definitions.

Finally we need to tell SBT to actually run this task at the appropriate moment. Since we need the compiled CSS output, it has to happen after the fact,
but it also has to happen before the application is finally put together so that the files are in place for packaging or running the app.

```scala
// run the copy after compile/assets but before managed resources
copyWebJarResources <<= copyWebJarResources dependsOn (compile in Compile, assets in Compile),
managedResources in Compile <<= (managedResources in Compile) dependsOn copyWebJarResources
```

NOTE! This whole procedure is IMHO a dirty hack, but couldn't find a better solution. If you do know a better way to do this, please create an issue.

## Running the server in a separate JVM

For fast development, it's nice to have the SBT console available even while the server is running. So instead of `spaJVM/run` you should use
the great [Revolver plugin](https://github.com/spray/sbt-revolver) and run the server with `re-start` and `re-stop`. This way you can start your
server and then instruct SBT to track changes to the client code with `~fastOptJS` and all your client changes are automatically deployed
without restarting your server.

To configure the Revolver you'll need the following

```scala
import spray.revolver.RevolverPlugin._
...
jvmSettings(Revolver.settings: _*).
...
// set some basic options when running the project with Revolver
javaOptions in Revolver.reStart ++= Seq("-Xmx1G"),
// configure a specific port for debugging, so you can easily debug multiple projects at the same time if necessary
Revolver.enableDebugging(port = 5111, suspend = false)
```

## Production build

So far we have been interested in having a nice and fast development cycle, without worrying about optimization. For your production release you'll want
to have a very optimized and small code-base to make the application perform well for the end user. To build an optimized version of the application
you just need to use `fullOptJS` instead of `fastOptJS`. This will produce a JavaScript file with `-opt.js` extension, so the server must know to serve
this file instead of the non-optimized version.

### Configuring between development and production

To achieve this, we need to add a configuration parameter that can be affected from outside the application. Typesafe provides a nice
[config library](https://github.com/typesafehub/config) for application configuration data so we'll be using it. The library automatically reads
`reference.conf` and `application.conf` (amongst a few others) from resources, so all you need to is to add an `application.conf` under `resources`.
Our configuration is extremely simple:

```yaml
spatutorial {
  productionMode = false
  productionMode = ${?PRODUCTION_MODE}
}
```

These configs can be overridden by having a separate `application.conf` in your built application conf-directory, but we are using a system
environment variable to override the single setting. By default `productionMode` is `false` but if the env variable `PRODUCTION_MODE` is set,
it will override the default.

`MainApp` loads this configuration into a `Config` object.

```scala
object Config {
  val c = ConfigFactory.load().getConfig("spatutorial")

  val productionMode = c.getBoolean("productionMode")
}
```

The `productionMode` parameter is only used to determine which HTML file to serve.

```scala
  if(Config.productionMode)
    getFromResource("web/index-full.html")
  else
    getFromResource("web/index.html")
```

### Using optimized versions of JS libraries

Typically JS libraries provided in WebJars come in two variants: normal and minified (`.min.js`). The latter is highly compressed and often also optimized
version of the more verbose normal version. For example debug-prints and development time checks have been removed. Therefore it makes sense to use these
pre-packaged minified versions instead of running the minification process yourself.

We need to define a separate list of JS dependencies for the production build, using the `.min.js` versions:

```scala
  val jsDependenciesProduction = Def.setting(Seq(
    "org.webjars" % "react" % "0.12.1" / "react-with-addons.min.js" commonJSName "React",
    "org.webjars" % "jquery" % "1.11.1" / "jquery.min.js",
    "org.webjars" % "bootstrap" % "3.3.2" / "bootstrap.min.js" dependsOn "jquery.min.js",
    "org.webjars" % "chartjs" % "1.0.1" / "Chart.min.js"
  ))
```

Because packaging the JS libs is a different task in SBT (it doesn't relate to `fastOptJS` or `fullOptJS`) we need another way to tell SBT to use these
dependencies instead of the normal ones. For that we'll define a `settingKey`

```scala
  val productionBuild = settingKey[Boolean]("Build for production")
```

and set it in the JS project

```scala
.jsSettings(
  productionBuild := false,
  jsDependencies ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesProduction.value},
```

To do a production build, you'll need to override the setting on SBT command line:

```
set productionBuild in js := true
```

If you now run `packageJSDependencies` it will use the minified versions and build a smaller `-jsdeps.js` file.

### Packaging an application

Running your app from SBT is fine for development but not so great in production. To build an application package with all the JAR libraries included,
we can use the [`sbt-native-packager`](https://github.com/sbt/sbt-native-packager) plugin.

```scala
// in plugins.sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.0-RC1")

// in build.scala
jvm.enablePlugins(JavaAppPackaging)
```

This is all you need to enable the `stage` command in SBT (under the JVM project), which will build and package the application for you in the
`target/universal/stage` directory. You may run the application with `bin/scalajs-spa` (or `bin\scalajs-spa.bat` in Windows).

We can configure the packager to set the `PRODUCTION_BUILD` environment variable automatically in the script with the following settings

```scala
NativePackagerKeys.batScriptExtraDefines += "set PRODUCTION_MODE=true",
NativePackagerKeys.bashScriptExtraDefines += "export PRODUCTION_MODE=true",
```

### Automating the release build

Even though we have all the pieces to build and package the application, it can be quite tedious to run several SBT commands one after another
to get everything done. That's what computers are really good at, so let's build a special `Command` to do the release.

```scala
val ReleaseCmd = Command.command("release") {
  state => "set productionBuild in js := true" ::
    "sharedProjectJS/test" ::
    "sharedProjectJS/fullOptJS" ::
    "sharedProjectJS/packageJSDependencies" ::
    "test" ::
    "stage" ::
    state
}
```
and enable it in the JVM project
```scala
  commands += ReleaseCmd,
```

With this command, you can just execute `release` under the `sharedProjectJVM` and SBT will run all those individual commands to build
your application package.

# FAQ

No questions asked so far!

# What next?

- debugging, source maps, logging
- authentication/authorization support
- you [tell me!](https://github.com/ochrons/scalajs-spa-tutorial/issues)
