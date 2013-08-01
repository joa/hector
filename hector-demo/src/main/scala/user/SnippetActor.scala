package user

import akka.actor.Actor

import java.util.Date

/**
 */
final class SnippetActor extends Actor {
  override def receive = {
    case "giveMeTheDate" ⇒
      sender ! <p>The current date is:
        {(new Date()).toString}
      </p>

    case "Message" ⇒
      import hector.js._
      import implicits._
      import toplevel.{jsWindow ⇒ window}

      sender ! ((window.status := "hello") & window.alert(JsString("Hello World! ")+(2 * window.status.length)))

    case publicKey ⇒
      sender ! <span>
        {publicKey}
      </span>
  }
}
