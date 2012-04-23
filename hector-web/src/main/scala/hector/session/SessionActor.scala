package hector.session

import hector.http._
import hector.session.backends._

import hector.Hector

import akka.actor._
import akka.pattern._
import akka.dispatch.Promise

object SessionActor {
  sealed trait SessionActorMessage

  case class Store[V <: Serializable](request: HttpRequest, key: String, value: V) extends SessionActorMessage

  case class Load(request: HttpRequest, key: String) extends SessionActorMessage

  private[session] val backend: Option[SessionBackend] = Hector.config.sessionBackend
}

/**
 *
 * {{{
 * sessionActor ! Store(request, "count", 0)
 * val count = (sessionActor ? Load(request, "count")).mapTo[Option[Int]]
 * }}}
 */
final class SessionActor extends Actor {
  import SessionActor._

  override protected def receive = {
    case Store(request, key, value) ⇒
      backend map { _.store(request, key, value) } getOrElse fail() pipeTo sender

    case Load(request, key) ⇒
      backend map { _.load(request, key) } getOrElse fail() pipeTo sender
  }

  private def fail() = {
    implicit val dispatcher = context.system.dispatcher

    //TODO(joa): perform additional logging and explain better.
    Promise.failed(new RuntimeException("No session backend defined.\nIf your web application requires state you have to override the sessionBackend variable in your configuration."))
  }
}
