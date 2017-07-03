# Dashboard

[대시 보드 모듈](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/modules/Dashboard.scala)은 React 내부 상태 또는 백엔드 기능을 포함하지 않으므로 이것은 기본적으로 두 개의 다른 구성 요소 인 Motd와 Chart에 대한 자리 표시자(placeholder)입니다. 유일한 메소드는 React에 의해 마운트 될 때 컴포넌트 렌더링을 담당하는`render` 메소드입니다. 또한 Chart 구성 요소에 가짜 데이터를 제공하여 작업을 단순하게 유지합니다.

```scala
// create the React component for Dashboard
private val component = ScalaComponent.builder[Props]("Dashboard")
  // create and store the connect proxy in state for later use
  .initialStateFromProps(props => State(props.proxy.connect(m => m)))
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
```

## Message of the day

[Motd component](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/Motd.scala) is a simple React component that shows a *Message of the day* from the server in a panel. The `Motd` is given the message in properties (wrapped in a `Pot` and a `ModelProxy`).

[Motd 구성 요소](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/Motd.scala)는 간단한 React 구성 요소로서 패널의 서버에서 보낸 *오늘의 메시지*  를 보여줍니다. `Motd`는 프로퍼티(`Pot`와`ModelProxy`로 싸인)에서 메시지를받습니다.
```scala
val Motd = ScalaComponent.builder[ModelProxy[Pot[String]]]("Motd")
  .render_P { proxy =>
    Panel(Panel.Props("Message of the day"),
      // render messages depending on the state of the Pot
      proxy().renderPending(_ > 500, _ => <.p("Loading...")),
      proxy().renderFailed(ex => <.p("Failed to load")),
      proxy().render(m => <.p(m)),
      Button(Button.Props(proxy.dispatchCB(UpdateMotd()), CommonStyle.danger), Icon.refresh, " Update")
    )
  }
  .componentDidMount(scope =>
    // update only if Motd is empty
    Callback.ifTrue(scope.props.value.isEmpty, scope.props.dispatchCB(UpdateMotd()))
  )
  .build
```
React 구성 요소는 일련의 함수 호출을 통해 정의됩니다. 이러한 각각의 호출은 컴포넌트의 타입을 수정합니다. 즉,`render`를 먼저 거치지 않으면`componentDidMount`를 호출 할 수 없습니다.

물론 실제로 메시지를 얻으려면 서버에서 메시지를 요청해야합니다. 컴포넌트가 마운트 될 때 자동으로이를 수행하기 위해, 우리는`componentDidMount` 메소드에서`dispatch`에 대한 호출을 후크한다.

`ModelProxy`와`Pot`의 사용에 대해서는 [나중에](todo-module-와 -data-flow.md) 자세히 다루겠습니다.

## Links to other routes

때로는 사용자를 경로 뒤에있는 다른 모듈로 이동시키는 링크를 만들어야합니다. 이 링크를 형식에 안전한 방식으로 생성하기 위해 튜토리얼은 'RouterCtl'의 인스턴스를 컴포넌트에 전달합니다.

```scala
ctl.link(TodoLoc)("Check your todos!")
```

