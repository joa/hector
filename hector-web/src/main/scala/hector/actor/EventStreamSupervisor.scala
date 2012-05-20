package hector.actor

import akka.util.Duration
import akka.pattern.ask
import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.util.duration._
import akka.util.Timeout

import hector.Hector
import hector.util._
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
  import context.dispatcher
  import akka.dispatch.Promise

  override val supervisorStrategy = OneForOneStrategy() {
    case _: Throwable ⇒
      // We stop any EventStreamActor because the client should retry the connection.
      // In that case we have to restart the actor, the client will retry the request
      // and all should live happily ever after.

      Restart
  }

  override protected def receive = {
    case Create(request, timeout, retry) ⇒
      //TODO(joa): only create if session exists
      val hash = randomHash()
      val actor = context.actorOf(Props(new EventStreamActor(timeout, retry)), name = hash)

      val eventStream =
        request.session map { _.set("hector:eventStream:"+hash, actor) } getOrElse Promise.successful(()) map {
          x ⇒ EventStream(urlOf(hash), actor)
        }

      request.session foreach { 
        _ onDestroy { 
          actor ! PoisonPill
        } 
      }

      eventStream pipeTo sender

    case message @ CreateResponse(request, Some(ReRoute(name))) ⇒
      import hector.http.status.NoContent

      val response =
        request.session match {
          case Some(session) ⇒
            val actorRefFuture =
              session[ActorRef]("hector:eventStream:"+name)

            actorRefFuture flatMap {
              case Some(actor) ⇒ ask(actor, message)(Timeout(5.seconds)) //TODO(joa): make configurable
              case None ⇒ Promise.successful(EmptyResponse(status = NoContent))
            }
          case None ⇒
            Promise.successful(EmptyResponse(status = NoContent))
        }
      

      response pipeTo sender
  }

  private def urlOf(name: String) =
    "/"+Hector.internalPrefix+"/es/"+name
}
