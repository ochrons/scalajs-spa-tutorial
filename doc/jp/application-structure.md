# アプリケーションの構造

本アプリケーションは、`client`、`server` そして`shared`、という３つのフォルダに分けられています。
それぞれの名前の通り、`client`にはSPAのクライアントの、`server` にはサーバーの、そして、`shared`には両者に共通に用いられる、コードとリソースがそれぞれ含まれます。
[`build.sbt`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/build.sbt) を見てみると、`crossProject`において、
Scala.js固有の [cross-building](http://www.scala-js.org/doc/sbt/cross-building.html)プロジェクト構造が定義されていることがわかります。

これらの各サブプロジェクト内においては、SBT/Scalaの通常のディレクトリ構造からの変更が行われます。
プロジェクトのビルドファイルの詳細については後に見ることとして、まずは実際のクライアントコードを見ていきましょう！
