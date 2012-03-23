package hector.session

import hector.http._
import hector.session.backends._

import hector.Hector

import akka.actor._
import akka.pattern._

object SessionActor {
  sealed trait SessionActorMessage

  case class Store[V <: Serializable](request: HttpRequest, key: String, value: V) extends SessionActorMessage

  case class Load(request: HttpRequest, key: String) extends SessionActorMessage

  //TODO(joa): configurable
  private[session] val backend: SessionBackend = new SessionRamBackend(Hector.system.dispatcher)
}

/**
 *
 * {{{
 * sessionActor ! Store(request, "count", 0)
 * val count = (sessionActor ? Load(request, "count")).mapTo[Option[Int]]
 * }}}
 * @author Joa Ebert
 */
final class SessionActor extends Actor {
  import SessionActor._

  override protected def receive = {
    case Store(request, key, value) ⇒ backend.store(request, key, value) pipeTo sender
    case Load(request, key) ⇒ backend.load(request, key) pipeTo sender
  }
}
