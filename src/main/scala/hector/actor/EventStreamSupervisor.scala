package hector.actor

import akka.util.{Timeout, Duration}
import akka.pattern.ask
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import akka.util.Timeout

import hector.Hector
import hector.util._
import akka.actor.{ActorRef, OneForOneStrategy, Props, Actor}
import hector.http.{EmptyResponse, HttpRequest}

object EventStreamSupervisor {
  case class Create(request: HttpRequest, timeout: Timeout, retry: Option[Duration] = None)
  case class ReRoute(name: String)

  case class EventStream(url: String, actor: ActorRef)
}

/**
 */
final class EventStreamSupervisor extends Actor {
  import EventStreamSupervisor._
  import akka.pattern.pipe
  import hector.session.SessionActor
  import context.dispatcher
  import akka.dispatch.Promise

  private[this] implicit val askTimeout = Timeout(5.seconds)

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Throwable ⇒
      // We stop any EventStreamActor because the client should retry the connection.
      // In that case we have to restart the actor, the client will retry the request
      // and all should live happily ever after.

      Restart
  }

  override protected def receive = {
    case Create(request, timeout, retry) ⇒
      val randomHashFuture =
        (Hector.utilities ? UtilityActor.NewRandomHash).mapTo[String]

      randomHashFuture flatMap {
        hash ⇒
          val actor = context.actorOf(Props(new EventStreamActor(timeout, retry)), name = hash)

          (Hector.session ? SessionActor.Store(request, "hector:eventStream:"+hash, actor)).mapTo[Unit] map {
            x ⇒ EventStream(urlOf(hash), actor)
          }
      } pipeTo sender

    case message @ CreateResponse(request, Some(ReRoute(name))) ⇒
      import hector.http.status.NoContent

      val actorRefFuture =
        (Hector.session ? SessionActor.Load(request, "hector:eventStream:"+name)).mapTo[Option[ActorRef]]

      val response =
        actorRefFuture flatMap {
          case Some(actor) ⇒ actor ? message
          case None ⇒ Promise.successful(EmptyResponse(status = NoContent))
        }

      response pipeTo sender
  }

  private def urlOf(name: String) =
    "/"+Hector.internalPrefix+"/es/"+name
}
