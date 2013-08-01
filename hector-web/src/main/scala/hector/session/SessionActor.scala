package hector.session

import hector.Hector

import akka.actor._
import akka.pattern._

import scala.compat.Platform
import scala.concurrent.Future

object SessionActor {
  sealed trait SessionActorMessage

  case class Store[V](id: String, key: String, value: V) extends SessionActorMessage

  case class Load(id: String, key: String) extends SessionActorMessage

  case class KeepAlive(id: String) extends SessionActorMessage

  private[session] val backend: Option[SessionBackend] = Hector.config.sessionBackend
}

/**
 *
 * {{{
 * sessionActor ! Store(id, "count", 0)
 * val count = (sessionActor ? Load(id, "count")).mapTo[Option[Int]]
 * }}}
 */
final class SessionActor extends Actor {
  import SessionActor._

  override def receive = {
    case Store(id, key, value) ⇒
      //TODO(joa): why do we need the implicit here?
      implicit val dispatcher = context.dispatcher
      backend map { _.store(id, key, value) } getOrElse fail() pipeTo sender

    case Load(id, key) ⇒
      //TODO(joa): why do we need the implicit here?
      implicit val dispatcher = context.dispatcher
      backend map { _.load(id, key) } getOrElse fail() pipeTo sender
    
    case KeepAlive(id) ⇒
      //TODO(joa): "hector:session:lastSeen" needs to be a constant somewhere accessible
      //TODO(joa): why do we need the implicit here?
      implicit val dispatcher = context.dispatcher
      backend map { _.store(id, "hector:session:lastSeen", Platform.currentTime) }
  }

  private def fail() = {
    implicit val dispatcher = context.system.dispatcher

    //TODO(joa): perform additional logging and explain better.
    Future.failed(new RuntimeException("No session backend defined.\nIf your web application requires state you have to override the sessionBackend variable in your configuration."))
  }
}
