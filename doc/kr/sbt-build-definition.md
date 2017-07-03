# SBT build definition

Scala.js는 아주 새롭고 최근에 발전해오고 있기 때문에 Scala.js 응용 프로그램을 SBT로 작성하는 것은 가능한 한 명확하지 않습니다. 예, 설명서 및 자습서는 기본 사항을 제공하지만 사용자 정의 디렉토리 레이아웃을 구성하는 것과 같은 것을 원한다면 무엇을 원하십니까?

이 튜토리얼의`build.sbt`는 당신이 당신 자신의 어플리케이션에서 실행할 수있는 몇 가지 전형적인 경우를 보여줍니다. `build.sbt`의 기본 구조는 Vincent Munier가 제공 한 [example] (https://github.com/vmunier/play-with-scalajs-example/blob/master/build.sbt) 상단에 구축되어 있습니다. , [sbt-play-scalajs] (https://github.com/vmunier/sbt-play-scalajs) 플러그인 작성자.

빌드는 세 개의 개별 프로젝트를 정의합니다.
* 공유 됨
* 고객
* 서버

## Shared project

첫 번째는 Scala.js`CrossProject`라는 특별한 프로젝트입니다. 실제로 JS와 JVM의 두 프로젝트가 있습니다. 이`shared` 프로젝트는 클라이언트와 서버간에 공유되는 클래스, 라이브러리 및 리소스를 포함합니다. 이 튜토리얼의 맥락에서`Api.scala` 특성과`TodoItem.scala` 사례 클래스를 의미합니다.
보다 현실적인 응용 프로그램에서는 여기에 데이터 모델 등을 정의해야합니다.

```scala
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.sharedDependencies.value
  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSPlay)
  .jsSettings(sourceMapsBase := baseDirectory.value / "..")
```

공유 된 의존성은 클라이언트 / 서버 통신을 위해`autowire`와`boopickle`과 같이 클라이언트와 서버가 모두 사용하는 라이브러리를 포함합니다.
```scala
val sharedDependencies = Def.setting(Seq(
  "com.lihaoyi" %%% "autowire" % versions.autowire,
  "me.chrons" %%% "boopickle" % versions.booPickle
))
```

## Client project

클라이언트는 ScalaJSPlugin을 활성화하여 일반적인 Scala.js 프로젝트로 정의됩니다.

```scala
lazy val client: Project = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.scalajsDependencies.value,
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
    jsDependencies ++= Settings.jsDependencies.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
    // must specify source maps location because we use pure CrossProject
    sourceMapsDirectories += sharedJS.base / "..",
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)
```

처음 몇 가지 설정은 일반적인 스칼라 설정이지만 나머지 설정을 통해 그들이하는 일을 설명합니다.

```scala
    // by default we do development build, no eliding
    elideOptions := Seq(),
    scalacOptions ++= elideOptions.value,
```
Eliding은 프로덕트 빌드에서 필요하지 않은 코드 (예 : 디버그 로깅)를 제거하는 데 사용됩니다. 이 설정은 기본적으로 비어 있지만 'release` 명령에서 사용 가능합니다.

```scala
    jsDependencies ++= Settings.jsDependencies.value,
    // RuntimeDOM is needed for tests
    jsDependencies += RuntimeDOM % "test",
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
```
`jsDependencies`는 애플리케이션이 의존하는 JavaScript 라이브러리 세트를 정의합니다. 이것들은 쉬운 소비를 위해 하나의`.js` 파일에 패키지되어 있습니다. `test` 단계에서는 Scala.js 플러그인이 테스트를 실행하기 위해 기본 Rhino 대신 PhantomJS를 사용하도록 RuntimeDOM을 포함시킵니다.
테스트를 실행하기 전에 [PhantomJS] (http://phantomjs.org/)를 설치했는지 확인하십시오.

```scala
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true,
    scalaJSUseMainModuleInitializer in Test := false,
```
이 설정은 특별한`launcher.js` 파일을 생성하도록 Scala.js 플러그인에게 알려주고 마지막에로드되고`main` 메소드를 호출합니다. 런처를 사용하면 HTML 템플릿을 깨끗하게 유지할 수 있습니다. 여기에서`main` 함수를 지정할 필요가 없습니다.

```scala
    // must specify source maps location because we use pure CrossProject
    sourceMapsDirectories += sharedJS.base / "..",
```
우리는 순수 CrossProject를 사용하기 때문에 소스 파일을 찾을 수있는 위치를 반영하도록 소스 맵 디렉토리를 수동으로 조정해야합니다.

```scala
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework")
```
SBT가 테스트를 위해 uTest 프레임 워크를 사용하고 있음을 알 수 있습니다.

```scala
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)
  .dependsOn(sharedJS)
```
Scala.js와 Play 용 Scala.js 플러그인을 모두 사용할 수 있습니다. 마지막으로`client` 프로젝트는 공유 코드와 리소스에 접근하기 위해`shared` 프로젝트에 의존해야합니다.

## Server project

서버 프로젝트는 클라이언트 통합을 쉽게하기 위해 몇 가지 단서가있는 일반적인 Play 프로젝트입니다. 무거워 짐의 대부분은`ScalaJSPlay` 플러그인에 의해 수행됩니다.이 플러그인은`PlayScala` 플러그인을 사용하여 모든 프로젝트에 자동으로 포함됩니다.

```scala
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.jvmDependencies.value,
    commands += ReleaseCmd,
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd),
    // compress CSS
    LessKeys.compress in Assets := true
  )
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)
```
클라이언트 프로젝트에서와 마찬가지로, 처음 몇 가지 설정은 일반적인 SBT 설정이므로 더 재미있는 설정에 집중하겠습니다.

```scala
    commands += ReleaseCmd,
```
새로운 SBT 명령`release`를 정의하여 일련의 명령을 실행하여 [배포 패키지] (production-build.md)를 생성합니다.
```scala
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages := Seq(scalaJSProd),
```
플러그인이 클라이언트 프로젝트가 어디에 있는지 알게하고 파이프 라인에서 Scala.js 처리를 가능하게하십시오.

```scala
    // compress CSS
    LessKeys.compress in Assets := true,
```
이것은 생성 된 CSS를 축소하기 위해`sbt-less` 플러그인을 지시합니다.


```scala
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
```
Play를 사용하지만 기본 레이아웃은 사용하지 않습니다. 대신 우리는`src / main / scala` 구조를 가진 일반적인 SBT 레이아웃을 선호합니다.

```scala
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)
```
서버는 클라이언트를 집계하며 공유 코드와 리소스에 액세스하기 위해`shared` 프로젝트에 의존합니다.
