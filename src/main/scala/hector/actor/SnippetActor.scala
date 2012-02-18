package hector.actor

import akka.actor.Actor
import java.util.Date

/**
 * @author Joa Ebert
 */
final class SnippetActor extends Actor {
  override protected def receive = {
    case "giveMeTheDate" =>
      Thread.sleep(32L)
      sender ! <p>The current date is:{" "+(new Date()).toString}</p>

    case publicKey =>
      Thread.sleep(32L)
      sender ! <span>{publicKey}</span>
  }
}
