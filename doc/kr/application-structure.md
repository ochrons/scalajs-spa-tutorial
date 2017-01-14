# 응용프로그램 구조

응용 프로그램은`client`,`server` 및`shared`의 세 폴더로 나뉩니다. 이름에서 알 수 있듯이 'client`는 SPA의 클라이언트 코드를 포함하고'server`는 서버이며
`shared`는 둘 다 사용되는 코드와 리소스를 포함합니다. [`build.sbt`] (https://github.com/ochrons/scalajs-spa-tutorial/tree/master/build.sbt)를 잠시 살펴보면,
이 Scala.js의 [cross-building] (http://www.scala-js.org/doc/sbt/cross-building.html) 프로젝트 구조를 정의하기 위해`crossProject`를 사용하게됩니다.

각 하위 프로젝트 내에서 일반적인 SBT / Scala 디렉토리 구조 규칙을 따릅니다.

프로젝트 빌드 파일에 대한 자세한 내용은 나중에 설명 하겠지만 실제 클라이언트 코드를 먼저 살펴 보겠습니다.

