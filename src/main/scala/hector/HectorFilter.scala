package hector

import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet.annotation.WebFilter
import akka.actor.{ActorRef, Props}
import hector.actor.RootActor
import scala.xml.Node

/**
 * @author Joa Ebert
 */
final class HectorFilter extends Filter {
  def init(filterConfig: FilterConfig) {
    // Warm-up:
    //

    Hector.system
    Hector.root
  }

  def destroy() {
    // Shutdown
    //

    Hector.system.shutdown()
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request match {
      case httpRequest: HttpServletRequest =>
        response match {
          case httpResponse: HttpServletResponse =>
            doFilter(httpRequest, httpResponse, chain)
          case _ =>
            chain.doFilter(request, response)
        }
      case _ =>
        chain.doFilter(request, response)
    }
  }

  private[this] def doFilter(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    if(httpRequest.isAsyncSupported) {
      filterAsync(httpRequest, httpResponse, chain)
    } else {
      filterSync(httpRequest, httpResponse, chain)
    }
  }

  private[this] def filterAsync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    // We try to enter the async state as soon as possible
    //

    val asyncContext = httpRequest.startAsync(httpRequest, httpResponse)

    // Add a listener to check if we handled the response.
    // If that is not the case we pass it to the chain.
    //

    asyncContext.addListener(
      new AsyncListener {
        def onError(event: AsyncEvent) {
          //TODO(joa): Should not happen but generate proper 500 just in case
        }

        def onTimeout(event: AsyncEvent) {
          //TODO(joa): Could happen so generate 500
        }

        def onStartAsync(event: AsyncEvent) {}

        def onComplete(event: AsyncEvent) {
          if(!event.getSuppliedResponse.isCommitted) {
            chain.doFilter(httpRequest, httpResponse)
          }
        }
      }
    )

    // Tell Hector we have an asynchronous context
    //

    Hector.root ! RootActor.HandleAsync(asyncContext)
  }

  private[this] def filterSync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    import akka.pattern.ask
    import akka.util.duration._
    import akka.util.Timeout
    import akka.dispatch.Await
    import java.util.concurrent.{TimeoutException => JTimeoutException}

    //TODO(joa): See Hector class. Timeout needs to be configurable
    implicit val timeout = Timeout(10.seconds)

    val t0 = System.currentTimeMillis()

    try {
      val future =
        Hector.root ? RootActor.HandleRequest(httpRequest, httpResponse)

      Await.result(
        awaitable = future.mapTo[Option[Node]],
        atMost = 10.seconds
      ) match {
        case Some(_) =>
        case None => chain.doFilter(httpRequest, httpResponse)
      }
    } catch {
      case timeout: JTimeoutException =>
        //TODO(joa): timeout needs to be logged. we need to generate a 505 response
      case _ =>
        // We have nothing to do here. RootActor should have already performed the necessary work.
    } finally {
      println("Completed sync request in "+(System.currentTimeMillis() - t0)+"ms.")
    }
  }
}
