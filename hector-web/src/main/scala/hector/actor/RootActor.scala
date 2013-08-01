package hector.actor

import akka.actor.{ActorLogging, OneForOneStrategy, Props, Actor}
import akka.actor.SupervisorStrategy.Restart
import akka.routing.{DefaultResizer, RoundRobinRouter}

import hector.session.SessionActor

/**
 * The RootActor is a super-visor for all of Hector's internal actors.
 */
final class RootActor extends Actor with ActorLogging {
  override def supervisorStrategy() =
    OneForOneStrategy() {
      case throwable: Throwable ⇒
        // For now we are going to restart all services automatically.
        Restart
    }

  override def receive = {
    case "run" ⇒
      import context.actorOf

      // The actor responsible for request handling

      log.debug("Created "+actorOf(
        Props[RequestActor].
          withRouter(
          RoundRobinRouter(
            resizer = Some(DefaultResizer(lowerBound = 32, upperBound = 64)))), name = "request"))

      // The actor responsible for session signals
      log.debug("Created "+actorOf(
        Props[SignalActor], name = "sessionSignals"))

      // The actor responsible for session handling

      log.debug("Created "+actorOf(
        Props[SessionActor].
          withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))), name = "sessionStorage"))

      // The actor responsible for JavaScript callbacks

      log.debug("Created "+actorOf(
        Props[CallbackActor].
          withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))), name = "callback"))

      // The actor responsible for statistics

      log.debug("Created "+actorOf(
        Props[StatisticsActor],
        name = "stats"))

      // The actor responsible for HTML 5 EventSource

      log.debug("Created "+actorOf(
        Props[EventStreamSupervisor],
        name = "eventStream"))
  }
}
