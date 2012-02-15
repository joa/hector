package hector

import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet.annotation.WebFilter
import akka.actor.{ActorRef, Props}

/**
 * @author Joa Ebert
 */
final class HectorFilter extends Filter {
  def init(filterConfig: FilterConfig) {
    Hector.system.actorOf(Props[Hector], name = "hector")
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request match {
      case httpRequest: HttpServletRequest =>
        response match {
          case httpResponse: HttpServletResponse =>
            doFilter(httpRequest, httpResponse, chain)
          case _ => chain.doFilter(request, response)
        }
      case _ => chain.doFilter(request, response)
    }
  }

  private[this] def doFilter(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    if(httpRequest.isAsyncSupported) {
      filterAsync(httpRequest, httpResponse, chain)
    } else {
      filterSync(httpRequest, httpResponse, chain)
    }
  }

  def destroy() {
    Hector.system.shutdown()
  }

  private[this] def filterAsync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    // We try to enter the async state as soon as possible
    //

    val asyncContext = httpRequest.startAsync(httpRequest, httpResponse)

    //TODO(joa): Pass to chain if we do not handle the request

    println("Do work Async")

    val hector =
      Hector.system.actorOf(Props[Hector])

    hector ! Hector.HandleAsync(asyncContext)
  }

  private[this] def filterSync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    import akka.pattern.ask
    import akka.util.duration._
    import akka.util.Timeout
    import akka.dispatch.Await

    //TODO(joa): Pass to chain if we do not handle the request

    //TODO(joa): See Hector class. Timeout needs to be configurable
    implicit val timeout = Timeout(10 seconds)

    val hector =
      Hector.system.actorOf(Props[Hector])

    Await.result(hector ? Hector.HandleRequest(httpRequest, httpResponse), 10 seconds)
  }
}
