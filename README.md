# Introdction
Hector is a <a href="http://www.scala-lang.org/">Scala</a> based web-framework which makes heavy use of <a href="http://www.akka.io/">Akka</a>.

Although Hector in its current state requires a Servlet 3.0 container it is ment to run stand-alone or in a Servlet 2.x+ container.

## Routing Requests

Incoming requests are routed via an internal actor. The user defines a `PartialFunction[HttpRequest, Route[Option[Any]]]` that tells Hector which Actor should handle a given request with an optional message parameter.
The message parameter is essential since it is the machanism to send arguments with a request.

Here is an example of a user-defined route:

```
def routes = {
  case HttpRequest(Get, "user" /: publicKey /: _) ⇒  Route(context.actorOf(Props[HelloWorldActor]), Some(publicKey))
}
```

This will match a GET request that starts with <em>user</em>, followed by an arbitrary string which is bound to the variable `publicKey` and ends with arbitrary data.
Here are some examples that are matched by this definition:

* /user/hector
* /user/hector/
* /user/hector/whatever
* /user/hector/whatever/

The path including <em>whatever</em> is matched because we did not specify how the request should end. Instead we used Scala's wildcard operator. 
If we want to match only <em>/user/hector/</eam>  we could use `"user" /: publicKey /: Required_/` and if we want to match <em>/user/hector</em> we would use `"user" /: publicKey /: No_/`.
`Required_/` and `No_/` are to important objects since they specify how a request is matched and clearly state wheter a slash is required or not.

If the user does not match a given request Hector will either show a 404-page or hand the request processing over to the servlet container if existing. This means a custom 404-page would be created by using `case _ ⇒ ...`.

### Route Timeout & Recovery

User-code can always specify a custom timeout for each route and a recovery strategy. If know recovery strategy is given Hector's default strategy is used which will display the error in development mode. The default timeout and recovery strategy can be configured in the future.

## Request Processing

Once Hector knows which actor should handle a response it will send the `CreateResponse` message to that actor with the given arguments. So in case of the route result `Route(context.actorOf(Props[HelloWorldActor]), Some(publicKey))` the message `CreateResponse(request, Some(publicKey))` would be dispatched to `HelloWorldActor`.
The `HelloWorldActor` is now responsible for generating an `HttpResponse` object and should reply it to its sender.

```
class HelloWorldActor extends Actor {
  val snippetActor = context.actorOf(Props[SnippetActor])

  implicit val timeout = Timeout(1.second)

  override def receive = {
    case CreateResponse(request, Some(publicKey: String)) ⇒
      val greetingFuture = (snippetActor ? publicKey)
      val jsCallback = Hector.callback ? NewCallback(request, snippetActor, "Message")

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

The most easy way to achieve this is to use Akka's pipe pattern. It is very important to note that we do not perform any blocking action here.
The user greeting could access a database and lookup the real-name for a given public key.

{TODO}
