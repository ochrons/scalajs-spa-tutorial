# CSS in Scala

이제 코드에서 HTML을 생성하는 것이 일반적입니다 (예를 들어 Scalatags 사용). 왜 스타일 시트에서도 똑같이하지 마십시오! 외부 CSS 파일을 사용하는 대신 코드에 스타일 시트를 작성하면 특정 이점이 있습니다. 하나의 명확한 이점은 조심하지 않으면 서로 쉽게 충돌하는 글로벌 클래스 이름을 제거하는 것입니다. 또한 타입 안전, 쉬운 리팩터링 및 IDE 완성 지원과 같은 것들을 얻을 수 있습니다.

이 글을 쓰고있을 때 Scala에서 CSS를 제작하기위한 적어도 두 개의 별도 라이브러리가 있습니다. 하나는 [Scalatags](https://github.com/lihaoyi/scalatags)에 내장되어 있고 다른 하나는 [ScalaCSS](https://github.com/japgolly/scalacss)라는 별개의 라이브러리입니다. 이들은 약간 다른 접근 방식을 취하므로 두 가지를 모두 확인하고 응용 프로그램에 적합한 것을 확인하는 것이 좋습니다. 이 튜토리얼에서는 ScalaCSS를 사용하고 있습니다. scalajs-react와 잘 통합되어 있기 때문입니다.

## Defining global styles

이 튜토리얼에서는 Bootstrap을 사용하여 대부분의 CSS를 제공하므로 전역 스타일 정의가 매우 간단합니다. 원본 CSS는 기본적으로 하나의 정의 만 포함합니다.

```css
body {
    padding-top: 50px;
}
```

이것을 ScalaCSS에서 표현하기 위해 우리는`StyleSheet.Inline` 클래스를 사용할 것입니다.

```scala
object GlobalStyles extends StyleSheet.Inline {
  import dsl._

  style(unsafeRoot("body")(
    paddingTop(50.px))
  )
}
```

더 자세한 예제는 [ScalaCSS documentation](https://japgolly.github.io/scalacss/book)을 참조하십시오.

`style`을 호출 할 때마다 내부 레지스트리에 새로운 스타일이 등록됩니다. 실제로 HTML 페이지에서 필요한 CSS를 생성하려면

```scala
GlobalStyles.addToDocument()
```

우리의 응용 프로그램 초기화 코드에서 실행하면 됩니다. 이것은 [scalajs-react에 대한 특정 초기화](https://japgolly.github.io/scalacss/book/ext/react.html)이며 다른 상황에서는 CSS를 만들고 삽입하는 다른 방법이 있습니다.

## Wrapping external CSS

우리가 사용하는 대부분의 스타일은 Bootstrap CSS에 정의되어 있으므로보다 편리하게 CSS에 액세스하려고합니다. 특히 어떤 시점에서 Bootstrap에서 [MaterializeCSS](http://materializecss.com/)로 전환하고 싶다면 모든 CSS 클래스 이름이 단일 위치에서만 발생하면 정말 좋을 것입니다.

부트 스트랩에서는 기본 클래스와 문맥 클래스를 사용하여 스타일을 정의하는 것이 매우 일반적입니다. 예를 들면 다음과 같습니다.

```html
<button class="btn btn-info">Info button</button>
```

스타일 래퍼를 만들기 위해 문맥 옵션과 헬퍼 함수를 정의하는 것으로 시작하겠습니다.

```scala
class BootstrapStyles(implicit r: mutable.Register) extends StyleSheet.Inline()(r) {

  import dsl._

  implicit val styleUnivEq: UnivEq[CommonStyle.Value] = new UnivEq[CommonStyle.Value] {}

  val csDomain = Domain.ofValues(default, primary, success, info, warning, danger)
  val contextDomain = Domain.ofValues(success, info, warning, danger)

  def commonStyle[A: UnivEq](domain: Domain[A], base: String) = styleF(domain)(opt =>
    styleS(addClassNames(base, s"$base-$opt"))
  )

  def styleWrap(classNames: String*) = style(addClassNames(classNames: _*))
```

`default`,`primary` 등의 값은`Bootstrap.scala` 구성 요소에 정의 된 열거 형에서옵니다. * domain *의 개념은 ScalaCSS [기능적 스타일] (http://japgolly.github.io/scalacss/book/features/stylef.html)에서 비롯되며 사용되기 이전에 생성 된 스타일에 대해 가능한 모든 값을 나열하는 방법입니다.

`commonStyle`은 정의 된 도메인으로부터 하나의 값을 입력 받아 적절한 스타일을 반환하는 * 기능적 스타일 *입니다. 우리는 가능한 모든 부트 스트랩`button` 스타일을 간단하게 정의 할 수 있습니다.

```scala
  val buttonOpt = commonStyle(csDomain, "btn")
  val button = buttonOpt(default)
```
기본 버튼 스타일은 간단한 사용을위한`button`으로 정의되어 있지만, * info * 버튼이 필요하다면 간단한 호출 인`buttonOpt (info)`가 그것을 제공 할 것입니다.

좀 더 직설적 인 부트 스트랩 스타일을 위해 우리는 단순히 제공된 모든 부트 스트랩 클래스 이름을 스타일에 추가하는`styleWrap` 함수를 사용합니다. 다양한 부트 스트랩 스타일을보다 명확하게 사용하기 위해 별도의 객체 아래에서 관련 스타일을 래핑합니다.

```scala
object listGroup {
  val listGroup = styleWrap("list-group")
  val item = styleWrap("list-group-item")
  val itemOpt = commonStyle(contextDomain, "list-group-item")
}
```

## Using styles

여러분의 React 컴포넌트에서 정의 된 인라인 스타일을 사용하려면 관련 implicit 변환을 얻기 위해`scalacss.ScalaCssReact._`를 import해야합니다.
그 후에는 스타일 시트에 대한 참조를 얻고 아래의 태그에 스타일을 사용하는 것만 큼 간단합니다.
```scala
private def bss = GlobalStyles.bootstrapStyles

val style = bss.listGroup
def renderItem(item: TodoItem) = {
  // convert priority into Bootstrap style
  val itemStyle = item.priority match {
    case TodoLow => style.itemOpt(CommonStyle.info)
    case TodoNormal => style.item
    case TodoHigh => style.itemOpt(CommonStyle.danger)
  }
  <.li(itemStyle)(
    <.input(^.tpe := "checkbox", ^.checked := item.completed, ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
    <.span(" "),
    if (item.completed) <.s(item.content) else <.span(item.content),
    Button(Button.Props(() => P.editItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Edit"),
    Button(Button.Props(() => P.deleteItem(item), addStyles = Seq(bss.pullRight, bss.buttonXS)), "Delete")
  )
}
<.ul(style.listGroup)(P.items toTagMod renderItem)
```

부트 스트랩 클래스 이름이 스칼라 메서드 뒤에 "숨겨져"있기 때문에 전체 IDE 코드 완성을 지원하며 클래스 이름을 잘못 입력 할 가능성이 없습니다.
컴파일러는 그것을 알아 차리지 못한다. 출력은 예상했던 것과 동일합니다.

```html
<ul class="scalacss-0029 list-group">
  <li class="scalacss-0034 list-group-item list-group-item-danger">
  .
  .
```
