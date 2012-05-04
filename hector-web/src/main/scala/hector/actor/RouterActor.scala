package hector.actor

import hector.Hector
import hector.actor.route.Route
import hector.http._
import hector.http.extractors._
import hector.util.letItCrash

import akka.actor.Actor

object RouterActor {
  private[RouterActor] val InternalPrefix = Hector.internalPrefix
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
