# Getting started

Fork a copy of the repository and clone it to your computer using Git. Run `sbt` in the project folder and after SBT has completed loading the project,
start the server with `re-start`. This will compile both the client and server side Scala application, package it and start the server. You can now navigate to
`localhost:8080` on your web browser to open the Dashboard view. It should look something like this

![dashboard](images/dashboard.png?raw=true)

The application is really simple, containing only two views (Dashboard and Todo) and you can access these by clicking the appropriate item on the menu. The Todo
view looks like this

![todos](images/todos.png?raw=true)

Now that you have everything up and running, it's time to dive into the details of what makes this application tick. Or if you want to experiment a little
yourself, use the `~fastOptJS` command on SBT prompt and SBT will automatically compile the (client side) application when you modify the source code. Try
changing for example the chart data in `js/Dashboard.scala` and reloading the web page.

