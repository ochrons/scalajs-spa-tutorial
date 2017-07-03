# Todo module and data flow

Todo 모듈과 React 구성 요소는 대시 보드보다 약간 재미 있습니다. 이 모듈은`TodoList` 컴포넌트를 포함하고 있으며, 서버로부터 검색된 Todo 아이템의리스트를 표시합니다. 사용자는 항목 옆에있는 확인란을 클릭하여 해당 항목이 완료되었는지 여부를 나타낼 수 있습니다. 내부적으로 Todo 아이템의 상태는`Todo` 모듈에 의해 유지되고`TodoList`는 수동적으로 표시됩니다.
실제 Todo 모듈 및 관련 구성 요소에 대해 자세히 알아보기 전에 Scala.js React 응용 프로그램에서 데이터 흐름에 대해 잠시 생각해 봅시다.

## Unidirectional data flow

밖에있는 여러 JS 프레임 워크 (예 : AngularJS)는 가변 상태와 양방향 데이터 바인딩을 사용합니다. 그러나이 튜토리얼에서는 단방향 데이터 흐름 및 불변 상태를위한 라이브러리 인 [Diode] (https://github.com/ochrons/diode)를 사용합니다. 이런 종류의 아키텍처는 양방향 데이터 바인딩으로 모든 종류의 어려운 문제가 발생할 수있는 복잡한 응용 프로그램에서 특히 잘 작동합니다. 비교적 단순한 개념이기 때문에 이와 같은 간단한 튜토리얼 애플리케이션에서도 잘 작동합니다. 아래에서 다이오드 아키텍처 다이어그램을 볼 수 있습니다.

![Diode architecture](https://github.com/ochrons/diode/raw/master/doc/images/architecture.png)

* Action *을 취하는 * Circuit *로 구성되어 Action Handler에 디스패치 한 다음 * Views *에 새로운 데이터로 자신을 업데이트하도록 알립니다. 자세한 내용은 [다이오드 문서](https://ochrons.github.io/diode)를 참조하십시오.

## Modifying a Todo state 

`TodoList` 컴포넌트는 Todo를 * completed *로 표시하는 데 사용할 수있는 각 Todo에 대한 체크 박스를 렌더링합니다.

```scala
<.input.checkbox(^.checked := item.completed, 
  ^.onChange --> P.stateChange(item.copy(completed = !item.completed))),
```

체크 박스를 클릭하면`stateChange` 메쏘드가 호출됩니다.이 메쏘드는`UpdateTodo (item)`액션을`SPACircuit`의 액션 핸들러에 보냅니다. 서킷에서 액션은`TodoHandler`의`handle` 함수에 의해 선택됩니다.이 함수는 내부 모델을 새로운 아이템으로 업데이트하고 그것을 서버로 보냅니다.


```scala
class TodoHandler[M](modelRW: ModelRW[M, Pot[Todos]]) extends ActionHandler(modelRW) {
  override def handle = {
    case UpdateTodo(item) =>
      val updateServer = () => AjaxClient[Api].updateTodo(item).call().map(UpdateAllTodos)
      // make a local update and inform server
      update(value.map(_.updated(item)), updateServer)
```
서버가 응답하면 서버에서 오는 데이터로 모델이 다시 업데이트됩니다. 업데이트는 같은`TodoHandler` 내에서 처리되는`UpdateAllTodos` 액션을 디스패치함으로써 간접적으로 발생합니다.

```scala
    case UpdateAllTodos(todos) =>
      // got new todos, update model
      update(Ready(Todos(todos)))
```

모델 업데이트는 청취자를 변경하기위한 호출을 트리거합니다. 실제로, 'Todos'에서 변경 사항을 관찰하는 두 개의 개별 구성 요소가 있습니다. 구성 요소 자체는 실제로 이러한 변경 사항을 수신하지 않지만 변경 사항은 구성 요소에 래핑됩니다. 이 랩퍼는 변경 사항에 반응하여 구성 요소를 갱신합니다. 이렇게하면 구성 요소에 대한 업데이트가 강제로 수행되며, 이는 다시 'render'를 호출하여 뷰를 새로 고칩니다. 변경 사항은 'TodoList'와 원래 클릭 된 개별 Todo로 연결됩니다.

그러나 앞서 언급했듯이 Todos의 변경에 관심이있는 다른 구성 요소가있었습니다. 이것은 Todo의 주 메뉴 항목으로 열려있는 Todos의 수를 표시합니다.

```scala
val todoCount = props.proxy().getOrElse(0)
Seq(
  <.span("Todo "),
  <.span(bss.labelOpt(CommonStyle.danger), bss.labelAsBadge, todoCount).when(todoCount > 0)
)
```

이것은 구성 요소가 변경의 출처 또는 변경에 관심이있는 사람을 알 필요가없는 단방향 데이터 흐름의 장점입니다. 모든 주 변경 사항은 이해 관계자에게 자동으로 전파됩니다.

다음으로, 데이터가 흐르도록 모든 것을 설정하는 방법을 살펴 보겠습니다.

## Wiring

**RootModel**
* todos 및 motd를 포함한 애플리케이션 모델을 나타냅니다.

**SPACircuit**
* 애플리케이션 모델과 액션 핸들러를 담고있는 Diode`Circuit [RootModel]의 싱글 톤 인스턴스.
* ReactConnector [RootModel]를 확장하여 React 구성 요소와의 통합을 제공합니다.

**TodoHandler**
* todos와 관련된 작업을 처리합니다.

**MotdHandler**
* Motd와 관련된 작업을 처리합니다.

우리의 React 컴포넌트가 애플리케이션 모델에 접근 할 수있게하려면,`SPACircuit.connect` 또는`SPACircuit.wrap` 메소드를 사용하여 연결해야합니다.

```scala
SPACircuit.wrap(_.motd)(proxy => Dashboard(ctl, proxy))
val todoWrapper = SPACircuit.connect(_.todos)
todoWrapper(Todo(_))
```

`wrap`과`connect`의 차이점은 전자가 모델과 디스패처에 대한 수동 읽기 액세스 만 제공하고 후자는 리스너를 회로에 등록하고 상태가 바뀔 때 래핑 된 구성 요소를 적극적으로 업데이트한다는 점입니다. 두 메소드 모두 관심있는 모델의 일부를 추출하는 _reader function_을 사용합니다.이 컴포넌트는 미리 빌드 된 다음`ModelProxy [A]`로 컴포넌트를 빌드하는 함수를 제공하여 인스턴스화됩니다.
`ModelProxy`는 추출 된 모델을 랩핑하고 디스패처에 대한 액세스를 제공합니다.

`Dashboard`에서 우리는`ModelProxy`의`connect` 메소드를 사용하여`Motd` 컴포넌트를 모델에 추가로 연결합니다.

```scala
.initialStateFromProps(props => State(props.proxy.connect(m => m)))
...
state.motdWrapper(Motd(_))
```

`Dashboard`는 모델의`motd` 부분만을 받았기 때문에 Motd 컴포넌트에 그대로 전달합니다.

## Working with potential data

`String` 대신에 Motd 컴포넌트가`Pot [String]`을 수신한다는 것을 알았을 것입니다. `Pot '은 여러 상태를 가질 수있는 데이터를 처리하는 데 유용한 구조입니다. Scala`Option`과 꽤 비슷하지만 두 개 이상의 상태가 있습니다.

![Pot states](http://ochrons.github.io/diode/images/pot-states.png)

잠재적 인 데이터를 보는 것은 번거로울 수 있습니다. 따라서 Diode는 'Pot'을 'PotReact'로 확장하는 편리한 암시 적 클래스를 포함합니다. 이것은`Pot '이 특정 상태에있을 때만 렌더링하는 특정`render` 메쏘드를 호출 할 수있게하여 사용자에게 "Loading"또는 "Error"메시지를 보여주기 쉽게합니다.

```scala
proxy().renderPending(_ > 500, _ => <.p("Loading...")),
proxy().renderFailed(ex => <.p("Failed to load")),
proxy().render(m => <.p(m)),
```

`renderPending`에는 두 가지 변형이 있습니다. 여기서는 필터링 된 것을 사용하고 있습니다. 첫 번째 매개 변수는 기간 값에 대한 필터입니다. 요청이 500ms 이상 보류 상태 인 경우에만 "로드 중 ..."메시지가 표시됩니다. 기본`render` 메소드는`Pot`의 내용을 렌더링합니다.

다양한`isEmpty`,`isPending`,`isFailed` 등의 메소드를 호출하여`Pot`의 상태를 질의하거나`map`과`flatMap`과 같은 모나드 함수를 사용하여 그것들을 조작 할 수 있습니다.

액션 핸들러 내에서는 autowire 호출과 같은 비동기 함수로부터받는 결과에 따라 스스로 'Pot'상태를 관리 할 수 있습니다. 그러나 일반적으로 요청이 보류 중일 때 자동 재시도 및 알림을 제공하는 모든 것을 처리하는 기성품 핸들러를 활용할 수 있습니다.

```scala
override def handle = {
  case action: UpdateMotd =>
    val updateF = action.effect(AjaxClient[Api].welcome("User X").call())(identity)
    action.handleWith(this, updateF)(PotAction.handler(3))
}
```

우선 우리가 수행하고자하는 비동기 호출로부터 _effect_를 생성하고, 그 결과를 자동적으로 관리되는`PotAction.handler`에 전달합니다.

다이오드에 대한 자세한 내용은 [documentation] (https://ochrons.github.io/diode)를 참조하십시오.

## Editing todos

새로운 Todo 항목을 추가하기 위해, 사용자 인터페이스는 버튼과 모달 대화 상자를 제공합니다 (앞에서 설명한`Modal` 컴포넌트 사용). 기존 항목 편집은 할 일 목록 설명 옆의 * 편집 * 버튼을 클릭하여 수행됩니다. 두 작업 모두 동일한 대화 상자를 엽니 다. 마지막으로 * 삭제 * 버튼을 클릭하여 할 일을 삭제할 수도 있습니다.

`TodoForm`은 사용자가 기존 Todo 아이템을 편집하거나 새로운 Todo 아이템을 만들 수있는 (Modal)을 기반으로하는 간단한 React 구성 요소입니다 (구성 요소의 관점에서이 둘 사이에 차이점이 없음). 그것은 다음과 같이 보입니다.

![Dialog box](images/dialogbox.png?raw=true)

대화 상자는 속성에서 선택 항목을 가져오고 현재 항목을 해당 상태로 유지합니다. `submitHandler` 콜백은 대화 상자가 닫히거나 취소 될 때 부모에게 알리기 위해 사용됩니다.

```scala
case class Props(item: Option[TodoItem], submitHandler: (TodoItem, Boolean) => Unit)

case class State(item: TodoItem, cancelled: Boolean = true)
```

Building the component looks a bit complicated, so let's walk through it.
```scala
val component = ScalaComponent.builder[Props]("TodoForm")
  .initialStateFromProps(p => State(p.item.getOrElse(TodoItem("", 0, "", TodoNormal, false))))
  .renderBackend[Backend]
  .build
  
def render(p: Props, s: State) = {
  log.debug(s"User is ${if (s.item.id == "") "adding" else "editing"} a todo")
  val headerText = if (s.item.id == "") "Add new todo" else "Edit todo"
  Modal(Modal.Props(
    // header contains a cancel button (X)
    header = hide => <.span(<.button(^.tpe := "button", bss.close, ^.onClick --> hide, Icon.close), <.h4(headerText)),
    // footer has the OK button that submits the form before hiding it
    footer = hide => <.span(Button(Button.Props(submitForm() >> hide), "OK")),
    // this is called after the modal has been hidden (animation is completed)
    closed = formClosed(s, p)),
    <.div(bss.formGroup,
      <.label(^.`for` := "description", "Description"),
      <.input.text(bss.formControl, ^.id := "description", ^.value := s.item.content,
        ^.placeholder := "write description", ^.onChange ==> updateDescription)),
    <.div(bss.formGroup,
      <.label(^.`for` := "priority", "Priority"),
      // using defaultValue = "Normal" instead of option/selected due to React
      <.select(bss.formControl, ^.id := "priority", ^.value := s.item.priority.toString, ^.onChange ==> updatePriority,
        <.option(^.value := TodoHigh.toString, "High"),
        <.option(^.value := TodoNormal.toString, "Normal"),
        <.option(^.value := TodoLow.toString, "Low")
      )
    )
  )
}
```

상태는 제공된 항목 또는 새 항목이있는 상태로 초기화됩니다. `render` 메쏘드 (`Backend`내부) 안에 새로운`Modal`이 생성되고 속성에서 두 개의 버튼 콘트롤을 할당합니다. `header`와`footer` 둘 다`Modal`의`Backend`에 실제로 주어져서`hide` 함수를 호출 할 수있는 함수입니다. 대화 상자가 숨겨지기 전에 양식이 먼저 제출됩니다.

양식 자체는 매우 간단하며 필드가 변경 될 때 내부 상태를 업데이트하는 핸들러가 있습니다. React에서`select` 엘리먼트는 일반 HTML5와 조금 다르게 작동하며 일반적인`selected` 속성 대신`value` 속성을 사용하여 옵션을 선택해야합니다.

폼이 닫히면 항목과 함께 대화 상자가 취소되었는지를 나타내는 플래그와 함께 부모의`submitHandler`가 호출됩니다.
```scala
def formClosed(state: State, props: Props): Callback = {
  // call parent handler with the new item and whether form was OK or cancelled
  props.submitHandler(state.item, state.cancelled)
}
```

그러나 이제는 클라이언트 - 서버 커뮤니케이션의 최하층에 도달 할 때입니다!