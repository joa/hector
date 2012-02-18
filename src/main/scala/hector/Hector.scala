package hector

import akka.pattern.ask
import akka.util.duration._

import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import akka.util.Timeout
import akka.routing.{DefaultResizer, RoundRobinRouter}
import akka.actor.{Props, Actor, ActorSystem}
import hector.actor.RootActor


/**
 * @author Joa Ebert
 */
object Hector {
  val system = ActorSystem("hector")

  val root =
    system.actorOf(
      Props[RootActor].
        withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))))
}
