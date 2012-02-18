package hector.actor

import akka.actor.{ActorRef, Props, Actor}
import akka.dispatch._
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.util.Timeout
import akka.util.duration._

import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import hector.http.HttpRequestConversion

import scala.xml._

object RootActor {
  sealed trait RootMessage
  case class HandleAsync(asyncContext: AsyncContext) extends RootMessage
  case class HandleRequest(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends RootMessage
  case class HandleResponse(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends RootMessage
}

/**
 * @author Joa Ebert
 */
final class RootActor extends Actor {
  import RootActor._

  private[this] val router =
      context.actorOf(
        Props[RouterActor].
          withRouter(
            RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))))

  //TODO(joa): Default request timeout (needs to be configurable)
  private[this] implicit val timeout = Timeout(10.seconds)

  override protected def receive = {
    case HandleAsync(asyncContext) =>
      import akka.pattern.pipe

      // We create an asynchronous context and pass it to
      // to the same actor to handle a normal request.
      //

      val t0 = System.currentTimeMillis()

      val request =
        self ? HandleRequest(
          asyncContext.getRequest.asInstanceOf[HttpServletRequest],
          asyncContext.getResponse.asInstanceOf[HttpServletResponse]
        )

      request onComplete {
        case _ =>
          println("Completed async request in "+(System.currentTimeMillis() - t0)+"ms.")
          asyncContext.complete()
      }

      request pipeTo sender

    case HandleRequest(httpRequest, httpResponse) =>
      import context.dispatcher
      import akka.pattern.pipe

      // We handle the request by asking the router for
      // the appropriate actor and message we have to
      // dispatch.

      val route =
        router ? HttpRequestConversion.fromHttpServletRequest(httpRequest)

      val response =
        (route flatMap {
          case Some((message, actor: ActorRef)) =>
            (actor ? message).mapTo[Node] map { Some(_) }
          case None =>
            // We are not participating in this request. Let someone
            // else handle this case.
            Promise.successful(None)
        }).mapTo[Option[Node]]

      //TODO(joa): Option[Node] should become something like HttpResponse
      response onComplete {
        case Right(Some(value)) =>
          httpResponse.getOutputStream.println(value.toString)
          httpResponse.getOutputStream.flush()
        case Right(None) =>
          // Nothing to do for us.
        case Left(error) =>
          //TODO(joa): Handling 500 should be configurable
          httpResponse.getOutputStream.println((<html><title>500!!!11!!!OMG</title><body>{error.getMessage}</body></html>).toString)
          httpResponse.getOutputStream.flush()
      }

      response pipeTo sender
  }
}
