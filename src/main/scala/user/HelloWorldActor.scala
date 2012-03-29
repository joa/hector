package user

import akka.actor.{Props, Actor}
import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._

import hector.Hector
import hector.http.{HtmlResponse, HttpRequest}
import hector.html.DocTypes
import hector.js.JsAST
import scala.xml.Node
import hector.actor.{CallbackActor, CreateResponse}

/**
 */
final class HelloWorldActor extends Actor {
  val snippetActor = context.actorOf(Props[SnippetActor])

  implicit val askTimeout = Timeout(10.seconds)

  protected override def receive = {
    case CreateResponse(request, Some(publicKey: String)) ⇒
      //val streamFuture = (Hector.eventStream ? EventStreamSupervisor.Create(request, 10.minutes)).mapTo[EventStream]
      val dateFuture = (snippetActor ? "giveMeTheDate")
      val greetingFuture = (snippetActor ? publicKey)
      val jsCallback = Hector.callback ? CallbackActor.NewCallback(request, snippetActor, "Message")

      val result =
        for {
          //stream ← streamFuture
          date ← dateFuture.mapTo[Node]
          greeting ← greetingFuture.mapTo[Node]
          callback ← jsCallback.mapTo[JsAST]
        } yield {
          //SomeState.room ! Join(stream.actor)

          /*
          <script type="text/javascript">{Unparsed("""
          var source = new EventSource('"""+stream.url+"""')
          source.addEventListener('message', function(e) {
            console.log(e.data);
          }, false)
                        """)}</script>
           */
          HtmlResponse(<html>
            <head>
              <title>Hector</title>
              {Hector.clientSupport}
            </head>
            <body>
              <h1>Hello
                {greeting}
              </h1>{date}<p>
              <a href="#" onclick={callback.emit()}>click me</a>
            </p>
            </body>
          </html>, DocTypes.`HTML 5`)
        }

      result pipeTo sender
  }
}
