package hector.actor

import hector.http.{HttpResponseOutput, EventStreamResponse}
import akka.actor.{ActorRef, Actor}
import akka.util.{Timeout, Duration}

object EventStreamActor {
  case class Event(data: String, id: Option[Int] = None, name: Option[String] = None)
}

/**
 * @see http://dev.w3.org/html5/eventsource/
 * @author Joa Ebert
 */
final class EventStreamActor(private[this] val timeout: Timeout, private[this] val retryOpt: Option[Duration] = None) extends Actor {
  // STATEFUL!

  import EventStreamActor._

  private[this] var outputOpt = Option.empty[HttpResponseOutput]

  private[this] var pending = List.empty[Event]

  override protected def receive = {
    case CreateResponse(request, _) ⇒
      // We are asked to create a response. Therefore we create an EventStream response.
      // This response is special in the way that it will send "self" a message which is
      // the actual HttpResponseOutput.

      sender ! EventStreamResponse(self, timeout)

    case output: HttpResponseOutput ⇒
      // Remember the output.

      outputOpt = Some(output)

      // Notify the client about our desired retry rate.

      retryOpt foreach { retry ⇒ output.println("retry: "+retry.toMillis) }

      // Dispatch all pending events in the order they have been received.

      pending.reverse foreach { self ! _ }
      pending = List.empty

    case event @ Event(data, idOpt, nameOpt) ⇒
      outputOpt match {
        case Some(output) ⇒
          try {
            idOpt foreach { id ⇒ output.println("id: "+id) }
            nameOpt foreach { name ⇒ output.println("event: "+name) }
            data.split('\n') foreach { line ⇒ output.println("data: "+line) }
            output.print('\n')
            output.flush()
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
