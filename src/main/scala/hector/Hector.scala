package hector

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.duration._

import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import akka.util.Timeout


/**
 * @author Joa Ebert
 */
object Hector {
  val system = ActorSystem("hector")

  sealed trait HectorMessage
  case class HandleAsync(asyncContext: AsyncContext) extends HectorMessage
  case class HandleRequest(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends HectorMessage
  case class HandleResponse(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends HectorMessage
}

/**
 * @author Joa Ebert
 */
final class Hector extends Actor {
  import Hector._

  //TODO(joa): Default request timeout (needs to be configurable)
  private[this] implicit val timeout = Timeout(10.seconds)

  override protected def receive = {
    case "ping?" =>
      println("Got ping!")
      sender ! "pong!"
    case HandleAsync(asyncContext) =>
      println("Handle Async")

      val t0 = System.currentTimeMillis()

      val request =
        self ? HandleRequest(
          asyncContext.getRequest.asInstanceOf[HttpServletRequest],
          asyncContext.getResponse.asInstanceOf[HttpServletResponse]
        )

      request onComplete {
        case _ =>
          println("Completed async request in "+(System.currentTimeMillis() - t0)+"ms")
          asyncContext.complete()
      }

    case HandleRequest(httpRequest, httpResponse) =>
      println("Handle Request")

      httpResponse.getOutputStream.println("Hello World!")
      httpResponse.getOutputStream.flush()

      sender ! true
  }
}
