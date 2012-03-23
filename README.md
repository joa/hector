# Introdction
Hector is a <a href="http://www.scala-lang.org/">Scala</a> based web-framework which makes heavy use of <a href="http://www.akka.io/">Akka</a>.

Although Hector in its current state requires a Servlet 3.0 container it is ment to run stand-alone or in a Servlet 2.x+ container.

# Philosophy
Some of the key concepts that Hector tries to fullfil.

##  State
The framework itself is trying to make very small use of stateful data. This data is stored with the session which makes its easy to understand.

## DSLs
There is no plan in writing DSLs for every single task. The key concept is that the user only needs to understand how to use Akka, not Hector.

However as a user you are able to import implicit conversions. That way `window.alert("Hello World")` creates an actual abstract syntax tree which can be emitted as minified JavaScript.

## Libraries
Since there are many good frameworks already available Hector will not come with an ORM. It is also debatable whether or not Hector itself will ever implement any templating mechanism.

There are four core parts:

* HTTP
* HTML/XML
* CSS
* JavaScript

Hector will not do anything special like serving an autogenerated 404 page. This way the user has complete control and can use Hector for websites or APIs.

# Akka
Because Hector is completely based on Akka its behavior is confugired the same way Akka is configured. The user decides whether or not certain components of the application should be redundant and on which machines they should stay. 
