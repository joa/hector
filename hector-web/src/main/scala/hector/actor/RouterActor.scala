package hector.actor

import hector.Hector
import hector.http._
import hector.http.extractors._
import hector.util.letItCrash

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout

object RouterActor {
  private[RouterActor] val InternalPrefix = Hector.internalPrefix

  /**
   * The Route reply.
   *
   * <p>A route defines an actor and additional arguments it will receive.</p>
   *
   * @param target The target actor.
   * @param arguments The arguments for the actor.
   * @param timeout The timeout for this route.
   * @param recover The recovery strategy.
   * @tparam A Type of arguments.
   */
  final case class Route[A](target: ActorRef, arguments: Option[A] = None, timeout: Timeout = Hector.config.defaultRequestTimeout, recover: Option[PartialFunction[Throwable, HttpResponse]] = None)
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

  private[this] val externalRoutes: PartialFunction[HttpRequest, Route[Any]] = Hector.config.routes

  private[this] val routes: PartialFunction[HttpRequest, Route[Any]] = internalRoutes orElse externalRoutes
}
