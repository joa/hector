package user

import akka.actor.Actor

import java.util.Date

/**
 * @author Joa Ebert
 */
final class SnippetActor extends Actor {
  override protected def receive = {

    case "giveMeTheDate" ⇒
      sender ! <p>The current date is:
        {(new Date()).toString}
      </p>

    case "Message" ⇒
      import hector.js._
      import JsImplicits._
      import JsToplevel.{jsWindow ⇒ window}

      sender ! ((window.status := "hello") & window.alert(2 * window.status.length))

    case publicKey ⇒
      sender ! <span>
        {publicKey}
      </span>
  }
}
