# Introdction
Hector is a <a href="http://www.scala-lang.org/">Scala</a> based web-framework which makes heavy use of <a href="http://www.akka.io/">Akka</a>.

Although Hector in its current state requires a Servlet 3.0 container it is ment to run stand-alone or in a Servlet 2.x+ container.

# Example

The user defines a PartialFunction[HttpRequest, Route[Option[Any]]] which tells Hector which Actor should handle a given request.

```
case HttpRequest(Get, "user" /: publicKey /: _) ⇒  Route(context.actorOf(Props[HelloWorldActor]), Some(publicKey))
```

This will match a GET request that starts with ```user```, followed by an arbitrary string which is bound to a variable and ends with or without a slash.

Here are some examples that are matched by this case:

* /user/hector
* /user/hector/
* /user/hector/whatever
* /user/hector/whatever/

The whatever is matched because we did not specify how the request should be matched exactly and we used the wildcard. If we want to match only 
/user/hector/ we could use ```"user" /: publicKey /: Required_/``` and if we only want to match /user/hector we would use ```"user" /: publicKey /: No_/```.
The ```Required_/``` and ```No_/``` are to important objects since they specify how a request is matched and clearly state wheter a slash is required or not.

{TODO}
