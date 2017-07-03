# Routing

SPA의 중요한 기능은 응용 프로그램 내의 "페이지"간의 탐색입니다. 물론 그것은 단일 페이지 * 응용 프로그램이기 때문에 실제 페이지가 아닙니다.
그러나 사용자 입장에서 보면 그렇게 보입니다. SPA의 일반적인 예는 브라우저의 URL이 애플리케이션의 상태를 반영하는 Gmail입니다.

서버에서 새 페이지를로드하지 않으므로 일반 브라우저 탐색을 사용할 수 없지만 직접 제공해야합니다. 이것은 라우팅이라고 불리우며 AngularJS와 같은 많은 JS 프레임 워크에 의해 제공됩니다. Scala.js 자체는 응용 프로그램 프레임 워크가 아니므로 Scala.js가 제공하는 준비된 라우터 구성 요소가 없습니다. 그러나 우리는 @japgolly와 같은 개발자들이 다른 모든 사람들에게 훌륭한 라이브러리를 제공하기 위해 모든 고통과 고통을 겪어 가도록하는 행운이 있습니다. 튜토리얼에서 저는``scalajs-react`와 잘 통합 된 [`scalajs-react` 라우터](https://github.com/japgolly/scalajs-react/blob/master/extra/ROUTER2.md)를 사용하고 있습니다. 경로를 관리하고 경로를 탐색 할 수있는 완벽한 방법을 제공합니다.

작동 원리는 기본적으로 경로 정의를 만들고 라우터에 등록한 다음 URL 변경에 따라 구성 요소를 바인딩하는 것입니다. 이 튜토리얼에서는 라우터를 사용하는 방법을 보여주기 위해 총 *2* 모듈/경로/보기(modules/routes/views)를 제공합니다.

이것은 단일 페이지 응용 프로그램이므로 모든 라우트는 하나의 라우터 구성 인 `routerConfig`로 정의됩니다.

```scala
val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
  import dsl._

  val todoWrapper = SPACircuit.connect(_.todos)
  // wrap/connect components to the circuit
  (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(m => m)(proxy => Dashboard(ctl, proxy)))
    | staticRoute("#todo", TodoLoc) ~> renderR(ctl => todoWrapper(Todo(_)))
    ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
}.renderWith(layout)
```

여기에서는`staticRoute`을 사용하여 두 경로를 등록합니다. 첫 번째 경로는 특별한 루트에 붙어있는 우리의 주 경로입니다. 두 번째 경로는`# todo` 경로에 첨부됩니다. 마지막으로 일치하지 않는 경로가 있으면 대시 보드로 리디렉션됩니다. 해시가없는 "깨끗한"경로를 사용할 수도 있지만 하위 경로가 정의되어 있어도 서버가 올바른 내용을 서버에 준비 할 수 있어야합니다.

라우터는 기본 HTML 코드 (레이아웃)를 제공하고 애플리케이션을위한`MainMenu` 컴포넌트를 통합합니다. SPA 튜토리얼은 부트 스트랩 CSS를 사용하여보기 좋은 레이아웃을 제공하지만 CSS 클래스 정의를 변경하여 원하는 CSS 프레임 워크를 사용할 수 있습니다.

```scala
import japgolly.scalajs.react.vdom.html_<^._
val todoCountWrapper = SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)
def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
  <.div(
    // here we use plain Bootstrap class names as these are specific to the top level layout defined here
    <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
      <.div(^.className := "container",
        <.div(^.className := "navbar-header", <.span(^.className := "navbar-brand", "SPA Tutorial")),
        <.div(^.className := "collapse navbar-collapse",
          // connect menu to model, because it needs to update when the number of open todos changes
          todoCountWrapper(proxy => MainMenu(c, r.page, proxy))
        )
      )
    ),
    // currently active module is shown in this container
    <.div(^.className := "container", r.render())
  )
}
```

타입 안전성을 제외하고 IDE가 자동 완성을 제공한다는 점을 제외하고 코드가 HTML과 어떻게 비슷한지 확인하십시오! HTML과 좀 더 닮았다 고 주장한다면`html _ <^ ^ '를`all`으로 대체하면 간단한'div`와`className` 태그 이름을 줄 수 있습니다. 그러나 HTML 네임 스페이스에`a`와`id '와 같은 짧은 공통 태그 이름이 많이 포함되어 있기 때문에 이것은 길을 따라 다니는 불쾌한 놀라움으로 이어질 수 있습니다. `<.`과`^ .`의 약간의 노력은 장기적으로 보상합니다.

