package hector.actor

import hector.Hector
import hector.http._
import hector.http.extractors._
import hector.util.letItCrash

import akka.actor.{Props, ActorRef, Actor}
import akka.util.Timeout
import akka.util.duration._
import akka.routing.{RoundRobinRouter, DefaultResizer}

import user.HelloWorldActor

object RouterActor {
  private[RouterActor] val InternalPrefix = Hector.internalPrefix

  /**
   * The Route reply.
   *
   * <p>A route defines an actor and additional arguments it will receive.</p>
   *
   * @param target The target actor.
   * @param arguments The arguments for the actor.
   * @tparam A Type of arguments.
   */
  final case class Route[A](target: ActorRef, arguments: Option[A] = None, timeout: Timeout = Timeout(3.seconds), recover: Option[PartialFunction[Throwable, HttpResponse]] = None)
}

/**
 */
final class RouterActor extends Actor {
  import RouterActor._

  override protected def receive = {
    case request: HttpRequest ⇒
      sender.tell(
        if(routes isDefinedAt request) {
          letItCrash()
          Some(routes(request))
        } else {
          None
        }
      )
  }

  private[this] val internalRoutes: PartialFunction[HttpRequest, Route[Any]] = {
    case Any(InternalPrefix /: "cb" /: callback /: _) ⇒ Route(Hector.callback, Some(CallbackActor.Execute(callback)))

    case Any(InternalPrefix /: "es" /: name /: _) ⇒
      Route(Hector.eventStream, Some(EventStreamSupervisor.ReRoute(name)))

    case Any(InternalPrefix /: "stats.txt" /: No_/) ⇒  Route(Hector.statistics)
  }

  private[this] val helloWorld = context.actorOf(Props[HelloWorldActor])//.withRouter(RoundRobinRouter(nrOfInstances = 128)))

  private[this] val externalRoutes: PartialFunction[HttpRequest, Route[Any]] = {
    case Get("user" /: publicKey /: _) ⇒  Route(helloWorld, Some(publicKey))
    //case _ ⇒ <html><title>404!</title><body>404</body></html>
  }

  private[this] val routes: PartialFunction[HttpRequest, Route[Any]] = internalRoutes orElse externalRoutes
}