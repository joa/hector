package hector.actor

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout

import hector.http.EventStreamResponse
import hector.http.io.Flush

import scala.concurrent.duration._

object EventStreamActor {
  case class Event(data: String, id: Option[Int] = None, name: Option[String] = None)
}

/**
 * @see http://dev.w3.org/html5/eventsource/
 */
final class EventStreamActor(private[this] val timeout: Timeout, private[this] val retryOpt: Option[Duration] = None) extends Actor {
  // STATEFUL!

  import EventStreamActor._

  private[this] var outputOpt = Option.empty[ActorRef]

  private[this] var pending = List.empty[Event]

  override def receive = {
    case CreateResponse(request, _) ⇒
      // We are asked to create a response. Therefore we create an EventStream response.
      // This response is special in the way that it will send "self" a message which is
      // the actual HttpResponseOutput.

      sender ! EventStreamResponse(self, timeout)

    case output: ActorRef ⇒
      // Remember the output.

      outputOpt = Some(output)

      // Notify the client about our desired retry rate.

      retryOpt foreach { retry ⇒ output ! ("retry: "+retry.toMillis+"\n") }

      // Dispatch all pending events in the order they have been received.

      pending.reverse foreach { self ! _ }
      pending = List.empty

    case event @ Event(data, idOpt, nameOpt) ⇒
      outputOpt match {
        case Some(output) ⇒
          try {
            idOpt foreach { id ⇒ output ! ("id: "+id+"\n") }
            nameOpt foreach { name ⇒ output ! ("event: "+name+"\n") }
            data.split('\n') foreach { line ⇒ output ! ("data: "+line+"\n") }
            output ! '\n'
            output ! Flush
          } catch {
            case _: Throwable ⇒
              // It is quite likely that a client might reconnect to an event source at some
              // time so we ignore premature end of file.
              //
              // In that case we will retry the message once the next output becomes available
              // but only if we do not die before.

              outputOpt = None
              pending = event :: pending
          }

        case None ⇒
          pending = event :: pending
      }
  }
}
