package user

import hector.actor.EventStreamSupervisor.EventStream
import hector.Hector
import akka.util.Timeout
import akka.pattern.ask
import hector.actor.{EventStreamActor, EventStreamSupervisor}
import hector.http.HttpRequest
import akka.actor.Props
import scala.concurrent.duration._

/**
 */
object SomeState {
  val room = Hector.system.actorOf(Props[ChatRoom])
}
