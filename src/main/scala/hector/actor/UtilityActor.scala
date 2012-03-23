package hector.actor

import akka.pattern.pipe
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.actor.Actor
import akka.dispatch.Future

import hector.util.{convertBytesToHexString, letItCrash}


/**
 * @author Joa Ebert
 */
object UtilityActor {
  sealed trait UtilityActorMessage

  /**
   * Creates and returns a new random hash of 32 bytes.
   */
  case object NewRandomHash extends UtilityActorMessage

  /**
   * Creates and returns a new random hash of 32 bytes preceded with a "_".
   */
  case object NewFunctionName extends UtilityActorMessage
}

final class UtilityActor extends Actor {
  import UtilityActor._
  import java.security.{SecureRandom ⇒ JSecureRandom}

  //XXX(joa): hashes are random, but not unique!

  private[this] val secureRandom: JSecureRandom = new JSecureRandom()

  private[this] implicit val implicitTimeout = Timeout(1.second)

  private[this] implicit val implicitContext = context.dispatcher

  override protected def receive = {
    case NewRandomHash ⇒
      letItCrash()

      val hash: Future[Array[Byte]] =
        Future {
          val bytes = new Array[Byte](32)
          secureRandom.nextBytes(bytes)
          bytes
        }

      hash pipeTo sender

    case NewFunctionName ⇒
      letItCrash()

      val hash =
        (self ? NewRandomHash).mapTo[Array[Byte]]

      val functionName: Future[String] =
        hash map {
          bytes ⇒
            val stringBuilder = new StringBuilder(65) // 32 bytes * 2 char per byte + 1 char ("_")

            stringBuilder append '_'

            convertBytesToHexString(stringBuilder, bytes)
        }

      functionName pipeTo sender
  }
}
