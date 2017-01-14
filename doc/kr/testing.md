# Testing

Scala.js 애플리케이션을 테스트하는 것은 자바 스크립트 환경의 한계로 괜찮은 테스트 프레임 워크를 선택해야한다는 점을 제외하고는 일반적인 스칼라 애플리케이션을 테스트하는 것처럼 쉽습니다. Specs2와 같은 많은 인기있는 프레임 워크는 JS 토지에서 사용할 수없는 JVM 기능 (예 : 리플렉션과 같은)에 의존하므로 Li Haoyi가 진행하여 간단하지만 강력한 [uTest] (https://github.com/lihaoyi/utest)를 만들었습니다 Scala.js와 훌륭하게 잘 작동하는 테스트 프레임 워크. ScalaTest는 이제 Scala.js (3.0.0-M5 버전 이상)에서 작동하도록 이식되어 있으므로 사용할 수있는 또 다른 옵션이 있습니다.

To define tests, you just need to extend from `TestSuite` and override the `tests` method.테스트를 정의하려면`TestSuite`에서 확장하고`tests` 메소드를 오버라이드해야합니다.

```scala
object SPACircuitTests extends TestSuite {
  override def tests = TestSuite {
    'test { ... }
  }
}
```

[`SPACircuitTests.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/test/scala/spatutorial/client/services/SPACircuitTests.scala)를보십시오. 테스트 케이스의 몇 가지 예가 있습니다.

SBT에서 테스트를 실행하려면, "com.lihaoyi"%%% "utest"% "0.3.1"`에 대한 의존성을 추가하고`testFrameworks + = new TestFramework ( "utest. 러너. 프레임 워크 ")`. 이제 SBT 프롬프트에서 일반적인`test` 및`testOnly` 명령을 사용하여 테스트를 실행할 수 있습니다.

