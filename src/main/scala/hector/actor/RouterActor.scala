package hector.actor

import hector.http._
import akka.actor.{Props, ActorRef, Actor}

/**
 * @author Joa Ebert
 */
final class RouterActor extends Actor {
  override protected def receive = {
    case request: HttpRequest =>
      sender.tell(
        if(routes isDefinedAt request) {
          Some(routes(request))
        } else {
          None
        }
      )
  }

  private[this] val routes: PartialFunction[HttpRequest, (Any, ActorRef)] = {
    case HttpRequest(Get, 'user /: publicKey /: _) =>
      publicKey.name -> context.actorOf(Props[HelloWorldActor])
    case HttpRequest(Get, 'error /: whatever /: _) =>
      (new RuntimeException(whatever.name)) -> context.actorOf(Props[HelloWorldActor])
    //case _ => <html><title>404!</title><body>404</body></html>
  }
}
