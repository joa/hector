package hector.actor

import akka.util.Timeout
import akka.util.duration._
import akka.actor.Actor
import akka.dispatch.Future

import hector.util.{convertBytesToHexString, letItCrash}
import akka.pattern.{AskTimeoutException, pipe, ask}

/**
 */
object UtilityActor {
  sealed trait UtilityActorMessage

  /**
   * Creates and returns a new random hash of 32 characters.
   */
  case object NewUniqueHash extends UtilityActorMessage

  /**
   * Creates and returns a new random hash of 32 characters preceded with a "_".
   */
  case object NewFunctionName extends UtilityActorMessage
}

final class UtilityActor extends Actor {
  import java.util.{UUID ⇒ JUUID}

  import UtilityActor._

  private[this] implicit val askTimeout = Timeout(1.second)

  private[this] implicit val implicitContext = context.dispatcher


  override protected def receive = {
    case NewUniqueHash ⇒
      letItCrash()

      val hash: Future[String] =
        Future {
          val uuid = JUUID.randomUUID().toString
          uuid.replace("-", "")
        }

      hash pipeTo sender

    case NewFunctionName ⇒
      letItCrash()

      val hashFuture =
        (self ? NewUniqueHash).mapTo[String] recover {
          case timeout: AskTimeoutException ⇒
            JUUID.randomUUID().toString.replace("-", "")
        }

      val functionName: Future[String] =
        hashFuture map {
          hash ⇒
            val stringBuilder = new StringBuilder(33) // 32 characters + preceding _

            stringBuilder.append('_')
            stringBuilder.append(hash)

            stringBuilder.toString()
        }

      functionName pipeTo sender
  }
}
