# Routing

SPAの重要な機能は、アプリケーション内の「ページ」との間の移動です。もちろん、これは、単一のページ・アプリケーションであるため、実際のページはありません。
しかし、ユーザーから見ればページ間の移動に見えます。SPAの一般的な例はGmailで、ブラウザのURLがアプリケーションの状態を反映しています。

サーバーから新しいページをロードしないため、一般的なブラウザナビゲーションを使用することができず、自分自身で提供する必要があります。これは、ルーティングと呼ばれAngularJSのような多くのJSフレームワークによって提供されています。Scala.js自体は、アプリケーションフレームワークではないので、ルータ用のコンポーネントは用意されていません。しかし、@japgollyのような開発者がすべての痛みや苦しみを経験し、優れたライブラリを提供しているので、我々は幸運です。このチュートリアルで私は`scalajs-react`とよく統合された[`scalajs-react`ルータ](https://github.com/japgolly/scalajs-react/blob/master/extra/ROUTER2.md)を使用しています。パスを管理してルートを探索するのに最適な方法を提供しています。

動作原理は、基本的にルート定義(route definitions)を作成し、ルータに登録し、URLの変更に応じてコンポーネントをバインドすることです。このチュートリアルでは、ルーターを使用する方法を示すために、合計　*2つの*　modules/routes/viewsを提供しています。

これはSPAなので、すべてのルートは1つのルータの設定である`routerConfig`に定義されます。

```scala
val routerConfig = RouterConfigDsl[Loc].buildConfig { dsl =>
  import dsl._

  (staticRoute(root, DashboardLoc) ~> renderR(ctl => SPACircuit.wrap(m => m)(proxy => Dashboard(ctl, proxy)))
    | staticRoute("#todo", TodoLoc) ~> renderR(ctl => SPACircuit.connect(_.todos)(Todo(_)))
    ).notFound(redirectToPage(DashboardLoc)(Redirect.Replace))
}.renderWith(layout)
```

ここでは、`staticRoute`を使用して２つのルートを登録します。最初のパスは、特殊な`root`に関連づけられるメインルート(main route)です。第二のパスは、`＃todo`パスに関連づけられます。いずれにも一致していないルートは、ダッシュボードにリダイレクトされます。ハッシュがない "クリーン"パスを使用することもできますが、サブパスが定義されていたとしても、サーバーは正しい内容をサーバーに用意する必要があります。

ルータは、基本的なHTMLコード(layout)を提供して、アプリケーションのための`MainMenu`コンポーネントを統合します。このチュートリアルは、Bootstrap CSSを使用して見栄えの良いレイアウトを提供していますが、CSSクラスの定義を変更することで、あなたが使用したいCSSフレームワークは何でも使うことができます。

```scala
import japgolly.scalajs.react.vdom.html_<^._
def layout(c: RouterCtl[Loc], r: Resolution[Loc]) = {
  <.div(
    // here we use plain Bootstrap class names as these are specific to the top level layout defined here
    <.nav(^.className := "navbar navbar-inverse navbar-fixed-top",
      <.div(^.className := "container",
        <.div(^.className := "navbar-header", <.span(^.className := "navbar-brand", "SPA Tutorial")),
        <.div(^.className := "collapse navbar-collapse",
          // connect menu to model, because it needs to update when the number of open todos changes
          SPACircuit.connect(_.todos.map(_.items.count(!_.completed)).toOption)(proxy => MainMenu(c, r.page, proxy))
        )
      )
    ),
    // currently active module is shown in this container
    <.div(^.className := "container", r.render())
  )
}
```

型安全性、IDEが自動補完を提供するという点を除いて、コードがHTMLとどれだけ似ているか確認してください！HTMLともっと似ていると主張する場合、 `html_<^`を`all`に置き換えるだけの簡単`div`と`className`タグ名を与えることができます。ただし、HTMLの名前空間には`a`や`id`のような短い共通タグ名が多く含まれているので、不快な驚きとなるかもしれないので気をつけてください。`<.`と`^.`の少しの努力は、長期的に補償します。
