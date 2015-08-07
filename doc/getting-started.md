# Getting started

Fork a copy of the repository and clone it to your computer using Git. Run `sbt` in the project folder and after SBT has completed loading the project,
start the Play server with `run`. You can now navigate to `localhost:9000` on your web browser to open the Dashboard view. At this point Play
will  compile both the client and server side Scala application, package it and run the server. This may take awhile, so monitor progress on your
SBT console. Dashboard should look something like this

![dashboard](images/dashboard.png?raw=true)

The application is really simple, containing only two views (Dashboard and Todo) and you can access these by clicking the appropriate item on the menu. The Todo
view looks like this

![todos](images/todos.png?raw=true)

Now that you have everything up and running, it's time to dive into the details of what makes this application tick. Or if you want to experiment a little
yourself, use the `~run` command on SBT prompt and Play will automatically re-compile the application when you modify the source code. Try
changing for example the chart data in `Dashboard.scala` and reloading the web page.

## Requirements

SPA Tutorial uses Play 2.4 which depends on Java 8, so make sure you are using JVM 8 or later.

Running client tests requires [PhantomJS](http://phantomjs.org/) to be installed.
