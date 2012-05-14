package hector.actor

import akka.actor.{ActorLogging, ActorRef, Actor}


object SignalActor {
  sealed trait SignalActorMessage
  case class Subscribe(target: ActorRef) extends SignalActorMessage
  case class Unsubscribe(target: ActorRef) extends SignalActorMessage
}

/**
 */
final class SignalActor extends Actor with ActorLogging {
  //STATEFUL!

  import SignalActor._

  private var subscribers: Seq[ActorRef] = Seq.empty

  override protected def receive = {
    case Subscribe(target) ⇒
      if(subscribers contains target) {
        log.warning("Actor {} has already been subscribed to {}.", target, self)
      } else {
        log.debug("Subscribing {} to {}.", target, self)
        subscribers = target +: subscribers
      }

    case Unsubscribe(target) ⇒ unsubscribe(target)

    case message ⇒
      val deadSubscriptions =
        for {
          target ← subscribers
        } yield {
          try {
            log.debug("Sending {} to {} from {}.", message, target, self)
            target ! message
            None
          } catch {
            case exception ⇒
              log.error(exception, "Could not send {} to {} from {}. Removing subscription.", message, target, self)
              Some(target)
          }
        }

      deadSubscriptions.flatten foreach unsubscribe
  }

  private[this] def unsubscribe(target: ActorRef) {
    log.debug("Unsubscribing {} from {}.", target, self)
    subscribers = subscribers filterNot { _ == target }
  }
}
