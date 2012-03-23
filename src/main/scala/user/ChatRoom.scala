package user

import akka.actor.{ActorRef, Actor}
import hector.actor.EventStreamActor


case class Join(actor: ActorRef)
case class Leave(actor: ActorRef)

/**
 * @author Joa Ebert
 */
final class ChatRoom extends Actor {
  private var members = Set.empty[ActorRef]

  private var counter = 0

  override def preStart() {
    import akka.util.duration._
    context.system.scheduler.schedule(0.seconds, 250.milliseconds, self, "dispatch")
  }

  override protected def receive = {
    case Join(actor) ⇒
      members = members + actor

    case Leave(actor) if members contains actor ⇒
      members = members - actor

    case "dispatch" ⇒
      val message = EventStreamActor.Event("Hello World! "+counter)

      counter += 1

      members foreach { _ ! message }
  }
}
