# Production build

지금까지 최적화에 대해 걱정하지 않고 멋지고 빠른 개발주기에 관심이있었습니다. 프로덕션 릴리스에서는 최종 사용자가 응용 프로그램을 제대로 수행 할 수 있도록 매우 최적화되고 작은 코드 기반을 원할 것입니다. 최적화 된 버전의 클라이언트 애플리케이션을 만들기 위해서는`fastOptJS` 대신`fullOptJS`를 사용해야합니다. 이렇게하면 확장명이`-opt.js` 인 JavaScript 파일이 생성되므로 서버는 최적화되지 않은 버전 대신이 파일을 제공해야합니다. 우리는 @vmunier의`play-scalajs-scripts`를 사용하여 HTML 템플릿 내에서 이것을 자동으로 수행합니다.

## Using optimized versions of JS libraries

일반적으로 WebJars에서 제공되는 JS 라이브러리에는 normal과 minified (`.min.js`)의 두 가지 변종이 있습니다. 후자는 압축률이 높으며 더 자세한 일반 버전의 최적화 된 버전입니다. 예를 들어, 디버그 - 인쇄 및 개발 시간 검사가 제거되었습니다. 따라서 직접 축소 프로세스를 실행하는 대신 미리 패키지화 된 이러한 사전 버전을 사용하는 것이 좋습니다.

`minified` 키워드를 사용하여 프로덕션 빌드에 대해 별도의 JS 종속성을 정의해야합니다.

```scala
/** Dependencies for external JS libs that are bundled into a single .js file according to dependency order */
val jsDependencies = Def.setting(Seq(
  "org.webjars.bower" % "react" % versions.react / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % versions.react / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
  "org.webjars" % "jquery" % versions.jQuery / "jquery.js" minified "jquery.min.js",
  "org.webjars" % "bootstrap" % versions.bootstrap / "bootstrap.js" minified "bootstrap.min.js" dependsOn "jquery.js",
  "org.webjars" % "chartjs" % versions.chartjs / "Chart.js" minified "Chart.min.js",
  "org.webjars" % "log4javascript" % versions.log4js / "js/log4javascript_uncompressed.js" minified "js/log4javascript.js"
))
```

Scala.js 플러그인은`fullOptJS`를 실행할 때 자동으로 축소 된 버전을 선택합니다.

## Packaging an application

Play는`dist` 명령으로 어플리케이션을 패키지하는 것을 매우 쉽게 만듭니다. 플러그인은 클라이언트가`fullOptJS`를 따르고 모든 관련 파일이 배포 자산으로 끝나게합니다.

## Automating the release build

응용 프로그램을 빌드하고 패키지화하는 데 필요한 모든 요소가 있지만 여러 가지 SBT 명령을 차례로 실행하여 모든 작업을 완료하는 것은 매우 지루할 수 있습니다. 이것이 바로 컴퓨터가 정말 잘하는 부분입니다. 따라서 이번에 출시 할 특별한`Command`를 만들어 보겠습니다. 여기에 우리는 클라이언트를위한`elideOptions`를 설정하여 디버그 코드를 제거합니다.

```scala
lazy val ReleaseCmd = Command.command("release") {
  state => "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
    "client/clean" ::
    "client/test" ::
    "server/clean" ::
    "server/test" ::
    "server/dist" ::
    "set elideOptions in client := Seq()" ::
    state
}
}
```

and enable it in the `server` project

```scala
  commands += ReleaseCmd,
```

이 명령을 사용하면`release`를 실행할 수 있으며, SBT는 모든 개별 명령을 실행하여 응용 프로그램 패키지를 빌드합니다.
