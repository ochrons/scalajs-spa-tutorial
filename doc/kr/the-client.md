# The Client

SPA의 경우 일반적으로 클라이언트는 단일 HTML 파일과 여러 가지 지원 리소스 (JS 및 CSS)로 구성됩니다. 이러한 리소스 중 하나는 Scala 소스에서 Scala.js에 의해 생성 된 실제 JavaScript 코드입니다. HTML은 Play 템플리트 시스템을 사용하여 서버 프로젝트 아래`twirl / views / index.scala.html`에 정의됩니다.
CSS 및 JS 파일에 대한 링크와 같은 일반적인 HTML 요소를 찾을 수 있습니다. 보시다시피, 모든 HTML은 응용 프로그램 자체에서 생성되므로 <body> 요소는 비어 있습니다. `@ scalajs` 지시어는 [ScalaJSWeb plugin](https://github.com/vmunier/sbt-web-scalajs)에게 생성 된 JavaScript 코드와 그 의존성을 적재하기 위해 적절한`<script>`태그를 삽입하도록 지시한다. 편의상, _asset 함수는 자산 (css, js, images)에 server/src/main/resources/application.conf에 정의 된 원하는 CDN 링크를 붙입니다. "$ {? APPLICATION_CDN}"이있는 행은 환경 변수가 정의 된 경우 프로덕션 환경의 기본값을 대체하는 한 가지 방법입니다. `_asset` 함수는 파일을 변경할 때 main.min.css를 main-`<version>`.min.css로 자동 변환하는 버전을 클라이언트 프록시와 브라우저 캐시를 지우는 파일 이름에 추가합니다. 이 모범 사례에 대한 자세한 내용은 [PlayFramework Fingerprinting](https://www.playframework.com/documentation/latest/Assets#Reverse-routing-and-fingerprinting-for-public-assets)을 참조하십시오.

```html
<body>
    <div id="root">
    </div>
    @scalajs.html.scripts(projectName = "client", name => _asset(name).toString, name => getClass.getResource(s"/public/$name") != null)
</body>
</html>
```

Instead of using external JavaScript references to [React](http://facebook.github.io/react/), [jQuery](http://jquery.com/), [Bootstrap](http://getbootstrap.com/) and
to a [chart component](http://www.chartjs.org/), the build system combines all these into a single JavaScript file (client-jsdeps.js). See 
[here](using-resources-from-webjars.md#webjar-javascript) for details. The last JavaScript reference is the compiled application code.

Once the browser has loaded all the resources, it will call the `SPAMain().main()` method defined in the
[`SPAMain.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/SPAMain.scala) singleton class. This is the 
entry point of the application. The class itself is very simple,

[React](http://facebook.github.io/react/), [jQuery](http://jquery.com/), [Bootstrap](http://getbootstrap.com), [차트 구성 요소](http://www.chartjs.org/) 외부 JavaScript 참조를 사용하는 대신 빌드 시스템은 이 모든 것을 하나의 JavaScript 파일 (client-jsdeps.js)로 결합합니다. 여기](using-resources-from-webjars.md # webjar-javascript)를 참조하십시오. 마지막 JavaScript 참조는 컴파일 된 응용 프로그램 코드입니다.

브라우저가 모든 리소스를로드하면, [`SPAMain.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/SPAMain.scala) 싱글 톤 클래스 내부의 `SPAMain().main()`메서드가 호출됩니다. 이것이 응용 프로그램의 진입점 이니다. 클래스 자체는 매우 간단합니다.

```scala
@JSExport("SPAMain")
object SPAMain extends JSApp {
  @JSExport
  def main(): Unit = {
    // 스타일 시트 생성
    GlobalStyles.addToDocument()
    // 라우터 생성
    val router = Router(BaseUrl.until_#, routerConfig)
    // 리액터에게 문서 body 에 라우터를 렌더하라고 말하기
    router().renderIntoDOM(dom.document.getElementById("root"))
  }
}
```

외부 접근 가능한 클래스와 함수는`@ JSExport` 주석이 달려 있으므로 Scala.js 컴파일러는이를 최적화하지 않고 전역 범위에서 정확한 이름으로 사용할 수 있도록합니다.

`main ()`이하는 일은 단순히 * router *를 생성하고 React에게 문서의 <div id = "root"> 태그 안에 렌더링하도록 지시하는 것입니다.

이제 이 시점에서 React에 대한 몇 가지 참조를 보았고 그 점에 대해 궁금해 할 것입니다. [React](http://facebook.github.io/react/)는 Facebook에서 개발 한 사용자 인터페이스를 구축하기위한 JavaScript 라이브러리입니다. "Scala가 대단하다면 Scala.js와 함께 JavaScript 라이브러리를 사용하는지"를 물어볼 수도 있습니다. 예, Scala.js에서 비슷한 것을 만드는 것이 좋을지도 모르지만, 이미 있습니다, 사용 안할 이유가 없어요. 그리고 달콤하게 다루기 위해 [David Barri (@japgolly)](https : // github.)의 [scalajs-react](https://github.com/japgolly/scalajs-react)라고하는 React를위한 아주 멋진 래퍼가 있습니다.

SPA를 구현하는 데 사용할 수있는 다른 Scala.js 라이브러리가 있지만 React와 함께 가고 싶었습니다. 그래서이 튜토리얼에서 사용할 것입니다. :)
