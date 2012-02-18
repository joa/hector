package hector.actor

import akka.actor.{Props, Actor}

import scala.xml.{Node, Text}

/**
 * @author Joa Ebert
 */
final class HelloWorldActor extends Actor {
  val snippetActor = context.actorOf(Props[SnippetActor])

  protected override def receive = {
    case publicKey: String =>
      import akka.pattern.pipe
      import akka.pattern.ask
      import akka.util.Timeout
      import akka.util.duration._

      implicit val timeout = Timeout(10.seconds)

      val future =
        for {
          date <- (snippetActor ? "giveMeTheDate").mapTo[Node]
          greeting <- (snippetActor ? publicKey).mapTo[Node]
        } yield {
          <html>
            <body>
              <title>Hector</title>
              <h1>Hello {greeting}</h1>
              {date}
            </body>
          </html>
        }

      future pipeTo sender

    case error: Throwable =>
      throw new Throwable(error)
  }
}
