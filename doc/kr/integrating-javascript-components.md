# Integrating JavaScript components

Scala.js는 웹 클라이언트를 개발할 수있는 우수한 환경을 제공하지만 때로는 수천 명의 JavaScript 개발자가 그림자 속에서 어설프게 다룰 수 있습니다 :)

## Bootstrap CSS components

[부트 스트랩](http://getbootstrap.com/)은 반응 형 응용 프로그램 개발을 위해 Twitter에서 널리 사용되는 HTML / CSS / JS 프레임 워크입니다. 여기에는 사용하기 쉽고 응용 프로그램에 통합되는 많은 스타일의 HTML / CSS 구성 요소가 함께 제공됩니다. Bootstrap의 많은 부분은 실제로 JavaScript를 사용하지 않으며, 모든 마법은 CSS에서 발생합니다.

이 튜토리얼은 간단한 부트 스트랩 (Bootstrap) [컴포넌트](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/Bootstrap.scala) (버튼 및 패널)을 React 구성 요소에 추가합니다. 부트 스트랩은 많은 구성 요소에서 문맥 스타일을 사용하여 추가 의미를 전달합니다. 이것들은 스칼라 열거로 쉽게 나타낼 수 있습니다.

```scala
object CommonStyle extends Enumeration {
  val default, primary, success, info, warning, danger = Value
}
```

대화식 버튼을 정의하려면 클릭 할 때 수행 할 작업을 알고 있어야합니다. 이 예제에서는 단순히 컨텍스트 스타일과 함께 속성에 함수를 전달합니다. 자식 구성 요소를 통해 정의하는 것이 더 편리하므로 실제 버튼 내용을 속성에 제공 할 필요가 없습니다.

```scala
object Button {

  case class Props(onClick: Callback, style: CommonStyle.Value = CommonStyle.default, addStyles: Seq[StyleA] = Seq())

  val component = ScalaComponent.builder[Props]("Button")
    .renderPC((_, props, children) =>
      <.button(bss.buttonOpt(props.style), props.addStyles.toTagMod, ^.tpe := "button", ^.onClick --> props.onClick, children)
    ).build

  def apply(props: Props, children: ReactNode*) = component(props)(children: _*)
  def apply() = component
}
```

이번에는 render 메소드가 두 개의 매개 변수를 얻는다 :이 컴포넌트에 주어진 속성과 자식. 단순히 부트 스트랩 CSS를 사용하여 정상적인 버튼을 렌더링하고 속성에서 정의한 핸들러에 onClick을 바인딩합니다. 마지막으로 버튼 태그 내에 자식이 렌더링됩니다.

부트 스트랩 패널 정의는 간단합니다.

```scala
object Panel {
  case class Props(heading: String, style: CommonStyle.Value = CommonStyle.default)

  val component = ScalaComponent.builder[Props]("Panel")
    .renderPC((_, p, c) =>
      <.div(bss.panelOpt(p.style),
        <.div(bss.panelHeading, p.heading),
        <.div(bss.panelBody, c)
      )
    ).build

  def apply(props: Props, children: ReactNode*) = component(props)(children: _*)
  def apply() = component
}
```

이 패널은 대화 형 기능을 제공하지 않지만 이번에는 children 속성을 사용하는 것 외에도 별도의 '제목'을 정의합니다.

## Icons

맞춤 글꼴은 모든 디스플레이에서보기 좋게 보이는 확장 가능한 아이콘을 생성하는 좋은 방법입니다. 이 튜토리얼에서는 [Font Awesome](http://fortawesome.github.io/Font-Awesome/) 아이콘과 적절한 HTML 태그를 생성하여 아이콘을 표시하는 간단한 래퍼를 사용합니다.

```scala
object Icon {
  type Icon = VdomNode
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

웹 UI에서 멋진 차트 구성 요소를 원한다면 많은 SVG 생성 코드를 작성할 수 있습니다.하지만 많은 이점을 제공 할 수있는 구성 요소가있는 경우 왜 그렇게해야할까요? Scala.js는 스칼라 코드에서 여러 가지 방법으로 [JavaScript 사용] (http://www.scala-js.org/doc/calling-javascript.html)을 제공하며 그 중 일부는 다른 것보다 더 유형에 안전합니다. 좋은 방법은 타사 JS 모듈과 노출 될 수있는 모든 데이터 구조에 대해 * facade *를 정의하는 것입니다. 이렇게하면 형식에 안전한 방식으로 사용할 수 있습니다.

이 튜토리얼에서 우리는 [Chart.js] (http://www.chartjs.org/)를 사용하고 있지만 실제적으로 모든 JS 구성 요소에 동일한 원칙이 적용됩니다.응용 프로그램에서 당신 자신이 직접 작성한 것 대신 Chart.js [기존 facade] (https://github.com/coreyauger/scalajs-chart)를 사용하는 것을 원할 수도 있습니다.

Chart.js는 HTML5 캔버스에 차트를 그리고 JavaScript 코드를 통해 인스턴스화됩니다.

```javascript
var ctx = document.getElementById("myChart").getContext("2d");
var myNewChart = new JSChart(ctx, { type: "line", data: data })
```

Scala.js에서 동일한 작업을 수행하기 위해 다음과 같이 간단한 * facade *를 정의합니다.

```scala
@js.native
@JSGlobal("Chart")
class JSChart(ctx: js.Dynamic, config: ChartConfiguration) extends js.Object

@js.native
trait ChartConfiguration extends js.Object {
  def `type`: String = js.native
  def data: ChartData = js.native
  def options: ChartOptions = js.native
}
```

차트를 실제로 인스턴스화하려면 캔버스 요소에 액세스해야하고 React를 사용하면 가상 DOM을 작성하고 장면 뒤의 실제 DOM을 업데이트하므로 조금 문제가 있습니다. 그러므로`render` 함수 호출시 캔버스 요소는 존재하지 않습니다. 이 문제를 해결하려면 실제 DOM이 업데이트 된 후에 호출되는 'componentDidMount` 함수로 차트를 만들어야합니다. 이 함수는`getDOMNode`를 통해 실제 DOM 노드에 접근 할 수있게 해주는`scope` 매개 변수와 함께 호출됩니다. 차트는`Chart`의 새로운 인스턴스를 생성하고 적절한 차트 함수를 호출함으로써 만들어집니다.

```scala
val Chart = ScalaComponent.builder[ChartProps]("Chart")
  .render_P(p =>
    <.canvas(VdomAttr("width") := p.width, VdomAttr("height") := p.height)
  )
  .componentDidMount(scope => Callback {
    // access context of the canvas
    val ctx = scope.getDOMNode.asInstanceOf[HTMLCanvasElement].getContext("2d")
    // create the actual chart using the 3rd party component
    scope.props.style match {
      case LineChart => new JSChart(ctx, ChartConfiguration("line", scope.props.data))
      case BarChart => new JSChart(ctx, ChartConfiguration("bar", scope.props.data))
      case _ => throw new IllegalArgumentException
    }
  }).build
```

Chart.js 입력 데이터는 아래의 JavaScript 객체입니다.

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

Scala.js에서 같은 것을 빌드하기 위해 우리는`js.Dynamic.literal`을 직접 사용할 수 있습니다.하지만 이것은 매우 안전하지 않고 번거로울 수 있습니다. 더 나은 대안은 빌더 함수를 정의하기 위해 빌더 함수를 정의하고 빌더 함수에 액세스하기위한 정면을 정의하는 것입니다.

```scala
@js.native
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

이 경우`ChartData` 특성을 정의하는 것은 사실상 필요하지 않습니다. 유형 안전성을 강화하는 것 이외에는 실제로 사용하지 않기 때문입니다. 그러나 실제로 애플리케이션 외부에서 정의 된 JavaScript 객체에 액세스해야하는 경우이를 수행하는 방법입니다. 차트 데이터 정의는 다음과 같이 간단합니다.

```scala
val cp = ChartProps("Test chart", Chart.BarChart, ChartData(Seq("A", "B", "C"), Seq(ChartDataset(Seq(1, 2, 3), "Data1"))))
```

매우 복잡한 JavaScript 객체를 작성 / 액세스해야하는 경우 [jducoeur](https://github.com/jducoeur) 에 의해 만들어진 [Querki](https://github.com/jducoeur/jsext/blob/master/src/main/scala/org/querki/jsext/JSOptionBuilder.scala))와 같은 옵션 작성기 방식을 고려하십시오.(예 : [JQueryUIDialog](https://github.com/jducoeur/Querki/blob/master/querki/scalajs/src/main/scala/org/querki/facades/jqueryui/JQueryUIDialog.scala)).

## Bootstrap jQuery components

부트 스트랩은 CSS 라이브러리 일뿐만 아니라 드롭 다운 및 모달과 같은 구성 요소에 기능을 추가하는 JavaScript도 함께 제공됩니다. [Modal](http://getbootstrap.com/javascript/#modals)은 모달이 활성화되고 숨겨진 후에 표시되는 숨겨진 대화 상자를 포함하므로 특히 문제가되는 시스템입니다. 일반적인 부트 스트랩 응용 프로그램에서는 대화 상자 HTML을 응용 프로그램의 일부로 정의하고 숨김 상태로 유지합니다. 그러나 React를 사용하면 응용 프로그램이 대화 상자의 내용을 쉽게 제어 할 수 있도록 표시되기 바로 전에 모달에 대한 HTML을 만드는 것이 쉽습니다 (권장).

Bootstrap Modal을 통합하기 전에 먼저 jQuery 컴포넌트를 통합하는 방법을 알아 보자. 우리는 진정으로 [스켈레톤 jQuery 통합] (https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/JQuery.scala)을 제공했습니다. , 모달이 작동하기에 충분하기 때문에, 대부분의 용도로 [더 완벽한] (https://github.com/jducoeur/jquery-facade) 무언가를 사용하고 싶을 것입니다. jQuery 통합은 [Scala.js documentation] (http://www.scala-js.org/doc/calling-javascript.html)에서도 간략하게 설명되어 있으므로 자세한 내용은 다루지 않을 것입니다. 기본적으로 전역`jQuery` 변수를 정의해야합니다.이 변수를 통해 jQuery 기능에 액세스 할 수 있습니다. 이것은 [`package.scala`] (https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/components/package.scala)에서 이루어집니다. ).

jQuery는 셀렉터 나 엘리먼트로 "호출"함으로써 작동한다. 이 자습서에서는 항상 직접 DOM 요소를 사용하므로 Facade에는 해당 옵션 만 포함됩니다. 예를 들어 이벤트 리스너를 요소에 연결하려면, 당신은 다음처럼 

```scala
jQuery(scope.getDOMNode).on("hidden.bs.modal", null, null, scope.backend.hidden _)
```

jQuery에는 플러그인이 jQuery 객체에 새로운 기능을 추가 할 수있는 확장 메커니즘이 있습니다. 예를 들어 부트 스트랩 모달은`모달`함수를 추가합니다. Scala.js에서 이러한 확장을 정의하기 위해이 확장을위한 특성과 그것을위한 암시 적 변환 (실제로 타입 캐스트 만)을 만듭니다.

```scala
@js.native
trait BootstrapJQuery extends JQuery {
  def modal(action: String): BootstrapJQuery = js.native
  def modal(options: js.Any): BootstrapJQuery = js.native
}

implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]
```

이제 우리가`jQuery (e) .modal () '을 호출하기를 원할 때마다 컴파일러는`JQuery` 타입을`BootstrapJQuery`에 자동으로 캐스팅합니다.

jQuery 통합으로 무장 한 이제 Modal 자체를 다룰 수 있습니다. 모달이 제기하는 문제 중 하나는 부트 스트랩 코드에 의해 동적으로 표시되고 숨겨 지므로 어떻게 든 제어해야한다는 것입니다. 이 튜토리얼에서는 모달이 필요하기 전에 존재하지 않고 모달이 생성 된 직후에 표시되는 디자인을 선택했습니다. 이것은 우리가 처리 할 수있는 숨어있는 부분만을 남깁니다.

`Modal`의`Backend`에서 우리는`hide ()`함수를 정의합니다.

```scala
class Backend(t: BackendScope[Props, Unit]) {
  def hide =
    // instruct Bootstrap to hide the modal
    t.getDOMNode.map(jQuery(_).modal("hide")).void
```

그러나 대화 상자 자체에는 대화 상자를 실제로 닫아야하는 컨트롤이 포함되어 있으므로이 기능을 속성을 통해 부모 구성 요소에 노출해야합니다.

```scala
// header and footer are functions, so that they can get access to the 
// hide() function for their buttons
case class Props(header: Callback => VdomNode, footer: Callback => VdomNode,
                 closed: Callback, backdrop: Boolean = true,
                 keyboard: Boolean = true)
```

또한 부트 스트랩 모달은 페이드 인 및 페이드 아웃되어 있으므로 부모는 DOM에서 모달 HTML을 즉시 제거 할 수 없지만 페이드 아웃이 완료 될 때까지 기다려야합니다. 이것은 이벤트를 듣고 나중에 부모의 'closed'함수를 호출하여 수행됩니다.
```scala
// jQuery event handler to be fired when the modal has been hidden
def hidden(e: JQueryEventObject): js.Any = {
  // inform the owner of the component that the modal was closed/hidden
  t.props.flatMap(_.closed).runNow()
}
...
// register event listener to be notified when the modal is closed
jQuery(scope.getDOMNode).on("hidden.bs.modal", null, null, scope.backend.hidden _)
```

생성 된 대화 상자를 보여주기 위해 우리는 다시`componentDidMount`의 jQuery를 통해`modal ()`을 호출합니다.
```scala
.componentDidMount(scope => Callback {
  val P = scope.props
  // instruct Bootstrap to show the modal
  jQuery(scope.getDOMNode).modal(js.Dynamic.literal("backdrop" -> P.backdrop, "keyboard" -> P.keyboard, "show" -> true))
```

