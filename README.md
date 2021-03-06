<img src="http://www.joa-ebert.com/files/hector.github.png" alt="Hector Logo"/>
# Introdction
Hector is a location transparent web-framework written in <a href="http://www.scala-lang.org/">Scala</a> which makes heavy use of <a href="http://www.akka.io/">Akka</a>.

Although Hector in its current state requires a Servlet 3.0 container it is ment to run stand-alone or in a Servlet 2.x+ container.

## Routing Requests

Incoming requests are routed via an internal actor. The user defines a `PartialFunction[HttpRequest, Route[Option[Any]]]` that tells Hector which Actor should handle a given request with an optional message parameter.
The message parameter is essential since it is the machanism to send arguments with a request.

Here is an example of a user-defined route:

```
val helloWorldActor = context.actorOf(Props[HelloWorldActor])
val helloWorldSelector = context.actorSelection(helloWorldActor.path)

def routes = {
  case Get("user" /: publicKey /: _) ⇒  Route(helloWorldSelector, Some(publicKey))
}
```

This will match a GET request that starts with *user*, followed by an arbitrary string which is bound to the variable `publicKey` and ends with arbitrary data.
Here are some examples that are matched by this definition:

* /user/hector
* /user/hector/
* /user/hector/whatever
* /user/hector/whatever/

The path including *whatever* is matched because we did not specify how the request should end. Instead we used Scala's wildcard operator. 
If we want to match */user/hector/* and nothing else  we could use `"user" /: publicKey /: Required_/`. If we want to match */user/hector* instead we would use `"user" /: publicKey /: No_/`.
`Required_/` and `No_/` are two important objects since they specify how a request is matched and clearly state wheter a slash is required or not.

If the user does not match a given request Hector will either show a 404-page or hand the request processing over to the servlet container if existing. This means a custom 404-page would be created by using `case _ ⇒ ...`.

### Route Timeout & Recovery

User-code can always specify a custom timeout for each route and a recovery strategy. If know recovery strategy is given Hector's default strategy is used which will display the error in development mode. The default timeout and recovery strategy can be configured in the future.

## Request Processing

Once Hector knows which actor should handle a response it will send the `CreateResponse` message to that actor with the given arguments. So in case of the route result `Route(helloWorldActor, Some(publicKey))` the message `CreateResponse(request, Some(publicKey))` would be dispatched to `HelloWorldActor`.
The `HelloWorldActor` is now responsible for generating an `HttpResponse` object and should reply it to its sender.

```
class HelloWorldActor extends Actor {
  val snippetActor = context.actorOf(Props[SnippetActor])

  implicit val timeout = Timeout(1.second)

  override def receive = {
    case CreateResponse(request, Some(publicKey: String)) ⇒
      val greetingFuture = (snippetActor ? publicKey)
      val jsCallback = Hector.callback ? NewCallback(request, context.actorSelection(snippetActor.path), "Message")

      val result =
        for {
          greeting ← greetingFuture.mapTo[Node]
          callback ← jsCallback.mapTo[JsAST]
        } yield {
          HtmlResponse(<html>
            <head>
              <title>Hector</title>
              {Hector.clientSupport}
            </head>
            <body>
              <h1>{greeting}</h1>
              <p>
                <a href="#" onclick={callback.emit()}>Click me</a>
              </p>
            </body>
          </html>, DocType.`HTML 5`)
        }

      result pipeTo sender
  }
}
```

The most easy way to achieve this is to use Akka's pipe pattern. The response processing is done by using custom code or some of Hector's built-in features like JavaScript callbacks which can be bound to logic on the server-side. 
All actions should happen in a non-blocking fashion. That means creating a greeting for the user by fetching it's name from the database is done in parallel while a new JavaScript callback is being created.

The JavaScript callback created with the `NewCallback` message will be bound to a session variable which stores the message that should be dispatched to a given actor. In this case `"Message"` is being dispatched to `snippetActor` when the JavaScript code is executed on click.

Hector comes with a set of built-in components like support for HTML5 EventSource.

### JavaScript Callbacks

A JavaScript callback is implemented as a message which will be sent to an actor. The actor may respond to that message with additional JavaScript code that is executed on the client or HTML which is appended to the &lt;body&gt; of the HTML document.

When user code requests a new callback via `Hector.callback ? NewCallback(request, target, message)`  Hector will create a session variable that establishes a connection between the callback on the client side and the `target` actor on the server side. Since writing to the session storage is a non-blocking operation creating a callback becomes a `Future[JsAST]`. The user can mix the result either with other JavaScript code or use it with an anchor tag.

Here is what the `snippetActor` from the earlier code snippet performs when it receives `"Message"`:

```
import hector.js._
import JsImplicits._
import JsToplevel.{jsWindow ⇒ window}
    
def receive = {
  case "Message" ⇒
    val response = ((window.status := "hello") & window.alert(2 * window.status.length))
    sender ! response
}
```

In this case the user will receive a message that displays "10". But what happens here? JavaScript is treated as a first-class citizen and Hector comes with a complete library of the JavaScript top-level.
Writing JavaScript with Hector should be as easy as possible and it is up to the user whether or not to use implicits or to build an AST manually.

Top-level variabes and functions are all prefixed with *js* so they do not accidentaly clash with user code or Scala's default imports like ```String```. 

`window` is a `JsObj` with the `JsWindowType` trait mixed in. `window.status` evaluates to a `JsMember` with the `JsStringType` mixed in. Since members can be bound to variables the `:=` method is defined which will evaluate to a `JsAssignment`. The list continues but it basically shows how easy it becomes to write JavaScript with Hector by making use of IDE auto-completion.

On a side-note: `JsAST` is not of type `HttpResponse`. Hector performs automatic response conversion for a set of types. Those include `JsAST`, `Seq[JsStatement]`, `Node` or `String`. However it is clearly stated which types are converted to a response. Only in development Hector will tell the developer which actor did not create a response suitable for automatic conversion.

## Development

### Pre-Commit Hook

In order to avoid stupid mistakes and enforce a consistent style one should enable Hector's pre-commit hook. 
It is a simple shell script which runs before each commit and makes sure that some constraints are valid.

The best idea is to create a symlink for the pre-commit hook. This is done by issuing the following command inside
the Hector repository.

```
ln -s pre-commit .git/hooks/pre-commit
```

## Configuration

Hector is configured by extending `hector.config.HectorConfig`. It is assumed that a configuration
is immutable and only one instance (per node) will ever be created.

The abstract `routes` method must always be overridden. A minimal configuration might look like this:

```
package hector

import akka.actor.ActorSystem

import hector.actor.route.Route
import hector.config.HectorConfig
import hector.http.extractors._
import hector.http._

final class Configuration extends HectorConfig {
  private[this] val system = ActorSystem("userCode")

  def routes = {
    case Any(_) ⇒ Route(system.actorSelection("/user/404"))
  }
}

```

Another very important configuration property is the session storage. The default option is using
an in-memory storage only suitable for development. If you go for a RESTful API you might want to
make sure to keep everything stateless via `override def sessionBackend = None`.

### Startup

By default Hector will try to create an instance of `hector.Configuration`. This class needs to be
provided by the user and it must extend `hector.config.HectorConfig`.

The configuration instance will be created after the `ActorSystem` of Hector has been created and before
any internal actor is started.

It is possible to change the class which Hector tries to instantiate for the configuration by providing
`-Dhector.config=foo.BarConfig` as a JVM argument.

## Fault Tolerance

{TODO}

## Clustering

{TODO}

## Dependencies

Hector requires at its heart only two dependencies to have a small foot-print. 

* [Akka](http://www.akka.io/)
  The foundation of Hector is Akka itself. Hector is location transparent because Akka is location transparent.
  Developing for Hector requires the user to understand how to use Akka.

* [Google Guava](http://code.google.com/p/guava-libraries/)
  The dependency on Guava is debatable and not absolutely necessary. However using Guava, which itself is a
  well written and tested library, makes many tasks like splitting text conciese and less error prone.
