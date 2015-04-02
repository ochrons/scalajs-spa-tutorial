# Application structure

The application is divided into three folders: `js`, `jvm` and `shared`. As the names imply, `js` contains the client code for the SPA, `jvm` is the server and
`shared` contains code and resources used by both. If you take a quick look at [`project/build.scala`](https://github.com/ochrons/scalajs-spa-tutorial/tree/master/project/build.scala) 
you will notice the use of `crossProject` to define this Scala.js specific [cross-building](http://www.scala-js.org/doc/sbt/cross-building.html) project structure.

Within each sub-project the usual SBT/Scala directory structure convention is followed.

We'll get to the details of the project build file later on, but let's first take a look at actual client code!

