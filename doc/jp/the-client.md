# The Client

一般的なSPAと同じように、clientはひとつのHTMLファイルといくつものリソースファイル（JSとCSS）によって構成されています。リソースのうちの一つはScalaコードからScala.jsによって生成された実際のJavaScriptコードです。  
HTMLはPlayのテンプレートシステムを利用しており、serverプロジェクト以下に `twirl/views/index.scala.html` として定義されています。CSSやJSファイルへのリンクなど、普通のHTMLによくあるものが見つかるでしょう。すべてのHTMLはアプリケーションによって生成されるため、見てのとおり`<body>`要素の中身はまったくの空です。  
`@playscalajs`ディレクティブは、生成されたJavaScriptおよびその依存関係にあるコードをロードする`<script>`タグを挿入するよう、[ScalaJSPlay plugin](https://github.com/vmunier/sbt-play-scalajs) に指示します。参考として、`_asset`関数は各アセット(css, js, images)に、server/src/main/resources/application.confで定義されているCDNへのリンクをプリフィックスとして付与します。"${?APPLICATION_CDN}"の行は、環境変数が設定されている場合に、その値で本番環境でのデフォルト値を上書きする一つの方法であることに注意してください。  
`_asset`関数は本番環境において、ファイルに変更を加えた際にクライアントのプロキシとブラウザキャッシュをクリアするために、main.min.cssをmain-`<version>`.min.cssと自動的に変換することでバージョン番号をファイル名に付加することも行います。  
より詳しい情報やベストプラクティスについては [PlayFramework Fingerprinting](https://www.playframework.com/documentation/latest/Assets#Reverse-routing-and-fingerprinting-for-public-assets)を参照してください。
  
```html
<body>
    <div id="root">
    </div>
    @playscalajs.html.scripts(projectName = "client", fileName => _asset(fileName).toString)
</body>
</html>
```
[React](http://facebook.github.io/react/)や[jQuery](http://jquery.com/)、[Bootstrap](http://getbootstrap.com/)、[chart component](http://www.chartjs.org/)といった外部のJavaScriptを参照する代わりに、ビルドシステムがそれらすべてを一つのJavaScriptファイル(client-jsdeps.js)に結合します。くわしくは[こちら](using-resources-from-webjars.md#webjar-javascript)を参照してください。  
最後のJavaScriptの参照はコンパイルされたアプリケーションコードです。  
  
ブラウザがすべてのリソースをロードすると、[`SPAMain.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/client/src/main/scala/spatutorial/client/SPAMain.scala)シングルトンクラスに定義されている`SPAMain().main()`を呼び出します。これがアプリケーションのエントリーポイントです。  
クラス自体はとてもシンプルです。
  
```scala
@JSExport("SPAMain")
object SPAMain extends JSApp {
  @JSExport
  def main(): Unit = {
    // create stylesheet
    GlobalStyles.addToDocument()
    // create the router
    val router = Router(BaseUrl.until_#, routerConfig)
    // tell React to render the router in the document body
    router().renderIntoDOM(dom.document.getElementById("root"))
  }
}
```
  
外部からアクセス可能なクラスや関数には`@JSExport`アノテーションが付与されているため、Scala.jsコンパイラはそれらを最適化によって除去せず、グローバルスコープ内で正しい名前で利用できるようにします。  
  
`main()`が行うのは、シンプルに*router*を生成し、ドキュメント内の`<div id="root">`タグにレンダリングするようReactに指示することです。  
  
ここまで何度か「React」の単語を目にして、あなたはそれが何なのか疑問に思ったかもしれません。[React](http://facebook.github.io/react/) はFacebookによって開発されたユーザーインターフェースを構築するためのJavaScriptライブラリです。  
「Scalaがとてもグレートなら、なぜJavaScriptのライブラリをScala.jsから使うんだ」とあなたは尋ねるかも知れません。同じようなScala.jsのライブラリでやるのが良さそうです。既にあるのでそちらを使うことにしましょう。[David Barri (@japgolly)](https://github.com/japgolly)による[scalajs-react](https://github.com/japgolly/scalajs-react)というとてもナイスなReactラッパーがあります。  
  
SPAを構築するのに使えるScala.jsのライブラリは他にもいくつかありますが、私はReactで行きたいので、チュートリアルではこれを使っていくことにします。:)