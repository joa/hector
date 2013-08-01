package hector.actor

import akka.actor.{OneForOneStrategy, Props, ActorRef, Actor}
import akka.actor.SupervisorStrategy.{Escalate, Stop}

import hector.Hector
import hector.actor.stats.ExceptionOccurred
import hector.http.io.{HttpRequestInputActor, HttpResponseOutputActor}

import java.io.{IOException, OutputStream ⇒ JOutputStream, InputStream ⇒ JInputStream}
import java.nio.charset.{Charset ⇒ JCharset}

/**
 */
private[actor] object IOActor {
  case class NewOutput(receiver: ActorRef, encoding: JCharset, output: JOutputStream)
  case class NewInput(receiver: ActorRef, encoding: JCharset, input: JInputStream)
}

private[actor] final class IOActor extends Actor {
  import IOActor._

  override val supervisorStrategy = OneForOneStrategy() {
    case exception: IOException ⇒

      //
      // When an IOException occurs we usually expect a closed connection and
      // it makes no sense to restart the actor.
      //

      Hector.statistics ! ExceptionOccurred(exception)
      Stop

    case _ ⇒

      //
      // Something other than an IOException happened. Therefore we are
      // no longer able to identify what we should do and escalate the
      // problem to the parent.
      //

      Escalate
  }

  override def receive = {
    case NewOutput(receiver, encoding, output) ⇒
      val outputActor = context.actorOf(Props(new HttpResponseOutputActor(encoding, output)))
      receiver ! outputActor

    case NewInput(receiver, encoding, input) ⇒
      val inputActor = context.actorOf(Props(new HttpRequestInputActor(encoding, input)))
      receiver ! inputActor
  }
}
