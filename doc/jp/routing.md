# Routing

A critical feature in a SPA is navigation between "pages" within the application. Of course they are not real pages, since it's a *Single Page* Application,
but from the user point of view it looks like that. A typical example of a SPA is Gmail where the URL in the browser reflects the state of the application.

Since we are not loading new pages from the server, we cannot use the regular browser navigation but need to provide one ourselves. This is called *routing* and
is provided by many JS frameworks like AngularJS. Scala.js itself is not an application framework so there is no ready made router component provided by it. But
we are lucky to have developers like @japgolly who go through all the pain and suffering to deliver great libraries for the rest of us. In the tutorial I'm
using [`scalajs-react` router](https://github.com/japgolly/scalajs-react/blob/master/extra/ROUTER2.md) which nicely integrates with `scalajs-react` and provides
a seamless way to manage routes and navigate between them.

The way it works is that you basically create route definitions, register them with the router and off it goes binding your components as the URL changes. In 
this tutorial we have a total of *two* modules/routes/views just to demonstrate how to use the router.

As this is a Single Page Application, all routes are defined under one router configuration, `routerConfig`.

```scala
val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
  import dsl._

  (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(m => m)(proxy => Dashboard(ctl, proxy)))
    | staticRoute("#todo", TodoLoc) ~> renderR(ctl => SPACircuit.connect(_.todos)(Todo(_)))
    ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
}.renderWith(layout)
```

Here we just register the two routes using the `staticRoute`. First route is our main route which is attached to the special `root`. The second
route is attached to `#todo` path. Finally any route that is not matched results in a redirect to the Dashboard. You could also use "clean" paths 
without the hash, but then your server must be prepared to server correct content even when there is a sub-path defined.

The router provides the base HTML code (layout) and integrates a `MainMenu` component for the application. SPA tutorial
uses Bootstrap CSS to provide a nice looking layout, but you can use whatever CSS framework you wish just by changing the CSS class definitions.

```scala
import japgolly.scalajs.react.vdom.prefix_<^._
def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
  <.div(
    // here we use plain Bootstrap class names as these are specific to the top level layout defined here
    <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
      <.div(^.className := "container",
        <.div(^.className := "navbar-header", <.span(^.className := "navbar-brand", "SPA Tutorial")),
        <.div(^.className := "collapse navbar-collapse",
          // connect menu to model, because it needs to update when the number of open todos changes
          SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)(proxy => MainMenu(c, r.page, proxy))
        )
      )
    ),
    // currently active module is shown in this container
    <.div(^.className := "container", r.render())
  )
}
```

See how the code looks just like HTML, except it's type safe and the IDE provides auto-completion! If you insist on having even closer resemblance to HTML,
you can replace the `prefix_<^` with `all` giving you simple `div` and `className` tag names. Be warned, however, that this may lead to nasty surprises
down the road because the HTML namespace contains a lot of short, common tag names like `a` and `id`. The little extra effort from `<.` and `^.` pays
off in the long run.

