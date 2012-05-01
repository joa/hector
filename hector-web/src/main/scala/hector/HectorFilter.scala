package hector

import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import hector.actor.RequestActor

/**
 */
final class HectorFilter extends Filter {
  def init(filterConfig: FilterConfig) {
    Hector.start()
  }

  def destroy() {
    Hector.stop()
  }

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
    request match {
      case httpRequest: HttpServletRequest ⇒
        response match {
          case httpResponse: HttpServletResponse ⇒
            //TODO(joa): create custom HttpRequest / HttpResponse object here
            doFilter(httpRequest, httpResponse, chain)

          case _ ⇒
            chain.doFilter(request, response)
        }

      case _ ⇒
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
    import akka.pattern.ask

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
          if(!event.getSuppliedResponse.isCommitted) {
            chain.doFilter(httpRequest, httpResponse)
          }
        }

        def onTimeout(event: AsyncEvent) {
          //TODO(joa): Could happen so generate 500
          if(!event.getSuppliedResponse.isCommitted) {
            chain.doFilter(httpRequest, httpResponse)
          }
        }

        def onStartAsync(event: AsyncEvent) {}

        def onComplete(event: AsyncEvent) {
          if(!event.getSuppliedResponse.isCommitted) {
            chain.doFilter(httpRequest, httpResponse)
          }
        }
      }
    )

    // Tell Hector we have an asynchronous context and act upon it.
    //

    (Hector.request ? RequestActor.HandleAsync(asyncContext))(asyncContext.getTimeout) onComplete {
      case _ ⇒ asyncContext.complete()
    }
  }

  private[this] def filterSync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    import akka.pattern.ask
    import akka.util.duration._
    import akka.util.Timeout
    import akka.dispatch.Await
    import java.util.concurrent.{TimeoutException ⇒ JTimeoutException}

    val t0 = System.currentTimeMillis()

    try {
      val future =
        ask(Hector.request, RequestActor.HandleRequest(httpRequest, httpResponse))(Hector.config.responseTimeout)

      Await.result(
        awaitable = future.mapTo[Option[Unit]],
        atMost = 10.seconds
      ) match {
        case Some(_) ⇒
        case None ⇒ chain.doFilter(httpRequest, httpResponse)
      }
    } catch {
      case timeout: JTimeoutException ⇒
        Hector.statistics ! ExceptionOccurred(timeout)
        //TODO(joa): timeout needs to be logged. we need to generate a 505 response
        
      case exception ⇒
        Hector.statistics ! ExceptionOccurred(error)
        // We have nothing to do here. RootActor should have already performed the necessary work.
    } finally {
      Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)
    }
  }
}
