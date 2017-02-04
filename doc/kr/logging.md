# Logging

우리 대부분은 서버 측에서 사용할 수있는 훌륭한 로깅 인프라에 익숙하며 때로는 직접 디버깅이 어렵거나 불가능할 수도 있습니다 (예 : 고객이 앱을 사용하는 경우). 다행히도 얇은 외벽을 통해 활용할 수있는 Javascript에 사용할 수있는 훌륭한 로깅 라이브러리가 있습니다.

log4j 스타일 API를 에뮬레이트하기 위해, 우리는`Logger` 인스턴스를 제공하는`LoggerFactory`를 정의 할 것입니다. 실제 기능을 제공하기 위해 기본 [Javascript 라이브러리](http://log4javascript.org/)에 연결됩니다. 자세한 내용은 [`Log4JavaScript.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/logger/Log4JavaScript.scala)를 참조하십시오.

패키지 객체 로거는 로깅 기능에 쉽게 접근 할 수 있도록`log`라는 기본 로거를 제공합니다. 별도의 로그 작성기를 작성하려면`LoggerFactory.getLogger (name)`메소드를 사용하십시오. 로그에 항목을 만들려면 메시지와 선택적 '예외'가있는 적절한 로그 수준 함수를 호출하면됩니다.

```scala
log.debug(s"User selected ${items.size} items")

log.error("Invalid response from server", ex)
```

기본 로거는 모든 메시지를 브라우저 콘솔에 인쇄하지만 고급 팝업 창 로거를 사용하여보다 세분화되고 필터링 된 로그 메시지를 분석 할 수도 있습니다. 그러한 로거를 생성하려면,`getPopupLogger`를 사용해주세요.

## Sending client logs to the server

또한 로깅 라이브러리는 모든 로그 메시지를 서버로 보내 오류 상황을보다 쉽게 분석 할 수있는 기능을 제공합니다. 각 로그 메시지를 작은 JSON 객체로 패키징하고 지정된 URL에 POST합니다. 서버 측에서는 이러한 로그 메시지를 수신하고 인쇄 할 경로를 정의합니다.

```scala
def logging = Action(parse.anyContent) {
  implicit request =>
    request.body.asJson.foreach { msg =>
      println(s"CLIENT - $msg")
    }
    Ok("")
}
```

서버 사이드 로깅을 가능하게하려면`log.enableServerLogging ( "/ logging")`을 호출하십시오. 서버 로그에서 클라이언트 로그 메시지는 다음과 같이 표시됩니다.

```
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500652089,"level":"INFO","url":"http://localhost:8080/#todo","message":"This message goes to server as well"}]
sharedProjectJVM Sending 4 Todo items
sharedProjectJVM Sending 4 Todo items
sharedProjectJVM Todo item was updated: TodoItem(3,Walk away slowly from an explosion without looking back.,TodoHigh,false)
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500661456,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500664865,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"Todo editing cancelled"}]
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500668485,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500671017,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500671751,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"User is editing a todo"}]
sharedProjectJVM CLIENT - [{"logger":"Log","timestamp":1425500672101,"level":"DEBUG","url":"http://localhost:8080/#todo","message":"Todo edited:
  TodoItem(3,Walk away slowly from an explosion without looking back.,TodoNormal,false)"}]
```

기본 로깅 라이브러리의 고급 기능 중 상당수는이 자습서 프로젝트에서 제공하지 않지만 현재 구현을 살펴보고 부족한 기능을 유용하게 찾을 수 있습니다.

## Limiting log messages in production

광범위한 디버그 메시지는 개발 과정에서 생명의 은인이지만 고객의 브라우저 콘솔을 디버그 메시지로 넘치고 싶지는 않습니다. 프로덕션 빌드에서 저수준 로그 메시지를 사용하지 않으려면 [@elidable] (http://www.scala-lang.org/api/current/index.html#scala.annotation.elidable)이라는 특수 스칼라 주석을 사용합니다. . 그것은 C / C ++에서`# ifdef`와 같이 함수를 제거하고 최종 바이트 코드에서 함수를 호출하는 것과 비슷합니다. 따라서 수백 개의`log.debug` 호출로 코드를 깔끔하게 처리하더라도 코드가 완전히 최적화되어 있기 때문에 성능상의 불이익은 없습니다.

```scala
  @elidable(FINEST) def trace(msg: String, e: Exception): Unit
  @elidable(FINEST) def trace(msg: String): Unit
  @elidable(FINE) def debug(msg: String, e: Exception): Unit
  @elidable(FINE) def debug(msg: String): Unit
  @elidable(INFO) def info(msg: String, e: Exception): Unit
  @elidable(INFO) def info(msg: String): Unit
```

제거 할 호출과 유지할 호출을 제어하려면`scalac` 명령 행 옵션`-Xelide-below <level>`을 사용하십시오. 이것은 자동적으로 해제 명령 내부에 있는 `WARNING`으로 설정됩니다..
```scala
// in settings we have scalacOptions ++= elideOptions.value,
set elideOptions in js := Seq("-Xelide-below", "WARNING")
```

프로덕션 빌드에서는 '경고'수준 아래의 모든 로그 호출이 최적화됩니다.
