# Using resources from WebJars

[WebJars] (http://www.webjars.org)는 JavaScript 라이브러리 및 CSS 정의와 같은 외부 리소스를 자신의 프로젝트에 쉽게 포함시킬 수있는 훌륭한 방법입니다. Bootstrap과 같은 JS / CSS 패키지를 다운로드하고 프로젝트 내에서 추출하는 대신 (또는 외부 CDN 서비스 리소스를 참조하는 것), 해당 WebJar에 종속성을 추가하면 모든 설정이 완료됩니다!

## WebJar JavaScript

Scala.js SBT 플러그인은 다양한 WebJars에서 JavaScript 소스를 추출하고이를 단일 JavaScript 파일로 연결하기위한 [훌륭하고 편리한 방법](http://www.scala-js.org/doc/sbt/depending.html)을 제공합니다. `index.html`에서 참조 할 수 있습니다. 튜토리얼 프로젝트에서 이는`build.scala` 파일의 다음 설정을 의미합니다 :

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

이렇게하면 모든 JavaScript 파일이 결합 된`client-jsdeps.js`라는 파일이 생성됩니다. 프로덕션 빌드에서는 각 JavaScript 파일의 최소화 된 버전이 선택됩니다.

## WebJar CSS/LESS

WebJars에서 CSS 파일을 추출하려면 아래에 설명 된 방법을 사용할 수 있지만 보너스로 [LESS] (http://lesscss.org/) 처리를 제공하는 좀 더 편리한 방법이 있습니다. 먼저 [plugins.sbt]에 [sbt-less](https://github.com/sbt/sbtless) 플러그인을 추가해야합니다.

```scala
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")
```

서버 프로젝트는`PlayScala` 플러그인을 사용하기 때문에`sbt-web`과`sbt-less` 플러그인을 자동으로 활성화시킵니다.

우리는 less 파일을`src / main / assets / stylesheets`에 저장하여 직접 복사 된 자원과 분리되도록합니다.

```scala
LessKeys.compress in Assets  := true,
```
이렇게하면 생성 된 CSS를 축소하기 위해 LESS 컴파일러에 지시합니다.

다음 단계는 WebJars 내부의 CSS / LESS 파일에 대한 참조를 사용하여`main.less` (예, 정확하게 지정해야 함) 파일을 만드는 것입니다.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
```

이 경우 우리는 다른 모든 CSS 스타일이 ScalaCSS를 사용하여 정의되었으므로 Bootstrap 및 Font Awesome Less 파일을 가져옵니다. WebJar에 따라 CSS 파일 외에도 LESS 파일이 포함되어있을 수도 있고 없을 수도 있습니다. LESS 파일을 사용하면`main.less` 파일에 CSS 변수를 정의하여 원하는대로 라이브러리 설정 (http://getbootstrap.com/css/#less)을 쉽게 할 수 있습니다.

```css
@import "lib/bootstrap/less/bootstrap.less";
@import "lib/font-awesome/less/font-awesome.less";
@brand-danger:  #00534f;
```

## WebJar resource files

때로는 WebJars에는 글꼴 Awesome 글꼴 파일과 같은 유용한 리소스가 포함되어 있습니다. 의존성으로 WebJar를 포함시키는 것만으로
우리는 추출한 내용을 직접 사용할 수 있습니다.
