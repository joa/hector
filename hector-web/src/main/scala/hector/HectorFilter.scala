package hector

import hector.actor.RequestActor
import hector.actor.stats.{RequestCompleted, ExceptionOccurred}

import javax.servlet._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
    // For now this will run in the global execution context.
    //

    (Hector.request ? RequestActor.HandleAsync(asyncContext))(timeout = asyncContext.getTimeout) onComplete {
      case _ ⇒ asyncContext.complete()
    }
  }

  private[this] def filterSync(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, chain: FilterChain) {
    import akka.pattern.ask
    import java.util.concurrent.{TimeoutException ⇒ JTimeoutException}

    val t0 = System.currentTimeMillis()

    try {
      val future =
        ask(Hector.request, RequestActor.HandleRequest(httpRequest, httpResponse))(Hector.config.responseTimeout)

      Await.result(
        awaitable = future.mapTo[Boolean],
        atMost = 10.seconds
      ) match {
        case true ⇒
        case false ⇒ chain.doFilter(httpRequest, httpResponse)
      }
    } catch {
      case timeout: JTimeoutException ⇒
        Hector.statistics ! ExceptionOccurred(timeout)
        //TODO(joa): timeout needs to be logged. we need to generate a 505 response
        
      case exception: Throwable ⇒
        Hector.statistics ! ExceptionOccurred(exception)
        // We have nothing to do here. RootActor should have already performed the necessary work.
    } finally {
      Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)
    }
  }
}
