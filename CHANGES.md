# Change history

## 1.1.3

* Major updates to dependencies:
  * Scala 2.11.8, Scala.js 0.6.8, SBT 0.13.11, scalajs-react 0.11.0, React 15.0.1, Play 2.5.1, ScalaCSS 0.4.1, Diode 0.5.1

## 1.1.2

* Updated to Diode 0.5.0, boopickle 1.1.2, scalajs-react 0.10.4 and scalajs-dom 0.9.0

## 1.1.0

* Switched from custom Flux/Rx architecture to [Diode](https://github.com/ochrons/diode)
* Updated to React 0.14 and scalajs-react 0.10.2

## 1.0.2

* Updated build file to support Scala IDE better

## 1.0.1

* Updated to 0.2.7 of `sbt-play-scalajs` to fix issue #20

## 1.0.0

* Server side is now on top of Play instead of Spray
* Simplified build file a lot
* Update to latest versions of libraries

## 0.1.10

* Switched from uPickle to [BooPickle](https://github.com/ochrons/boopickle)

## 0.1.9

* Style definitions are now done with [ScalaCSS](https://github.com/japgolly/scalacss/)
* Documentation is now in a separate [GitBook](http://ochrons.github.io/scalajs-spa-tutorial/)

## 0.1.8

* Upgraded many libraries to their latest versions
* Changed how the MainRouter is initialized and used to make it more convenient

## 0.1.7

* Support for logging on the client side (also delivers log messages to the server!)
* Source maps are served by the web server, to enable debugging with original source files on Chrome

## 0.1.6

* Added production build features
* Updated to Scala.js 0.6.1 and scalajs-react 0.8.1

## 0.1.5

* Cleaner SBT build definition (credits to @PerWiklander)
* Managing JS, CSS and other resources with WebJars

## 0.1.4

* Introduced [ScalaRx](https://github.com/lihaoyi/scala.rx) to propagate changes from store to views, replaced EventEmitter

## 0.1.3

* Unidirectional data flow framework *Ukko* following Facebook Flux and actor architectures
* Todo list implemented with the new data flow model
* Main menu item *Todo* now shows count of open todos
* Testing with *uTest*

## 0.1.2

* Simple jQuery integration added (Bootstrap Modal)
* Modal example with a form
* Refactored Bootstrap components a bit
* Todos are now updated on the server

## 0.1.1

* Refactored the router system to follow the intended design of the Scala.js React router (thanks to @japgolly for feedback)
* Separated MainMenu into its own component as part of the router refactoring
* Updated libraries scalajs-dom and scalajs-react to 0.8.0
* Changed all tags to use <^ prefixes (less potential for name conflicts)
* Documentation updated to reflect the changes

## 0.1.0

* Initial release
