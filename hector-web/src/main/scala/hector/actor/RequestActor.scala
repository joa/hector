package hector.actor

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.dispatch._
import akka.pattern.{AskTimeoutException, ask}
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.util.Timeout

import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import hector.Hector
import hector.util.letItCrash
import hector.actor.stats.ExceptionOccurred
import hector.config.RunModes
import hector.http.{HttpResponse, HttpSession}

object RequestActor {
  sealed trait RootMessage
  final case class HandleAsync(asyncContext: AsyncContext) extends RootMessage
  final case class HandleRequest(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends RootMessage
}

/**
 */
final class RequestActor extends Actor with ActorLogging {
  import RequestActor._

  private[this] val router =
      context.actorOf(
        Props[RouterActor].
          withRouter(
            RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))))

  override protected def receive = {
    case HandleAsync(asyncContext) ⇒
      import akka.pattern.pipe
      import akka.pattern.AskTimeoutException
      import stats.RequestCompleted

      letItCrash()

      val t0 = System.nanoTime()

      val request =
        ask(self, HandleRequest(
          asyncContext.getRequest.asInstanceOf[HttpServletRequest],
          asyncContext.getResponse.asInstanceOf[HttpServletResponse]
        ))(Hector.config.responseTimeout)

      request onComplete {
        case Right(Some(_)) ⇒
          Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)

        case Right(None) ⇒
          // Nothing to do. We did not participate.

        case Left(error) ⇒
          Hector.statistics ! ExceptionOccurred(error)
          Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)

          log.error(
            error match {
              case askTimeout: AskTimeoutException ⇒ "Timeout. Did you forget to reply in your actor?"
              case other ⇒ "Could not fulfill request due to an unexpected error."
            }
          )
      }

      request pipeTo sender

    case HandleRequest(httpRequest, httpResponse) ⇒
      import akka.pattern.pipe
      import context.dispatcher
      import hector.actor.RouterActor.Route
      import hector.http.{OutputStreamHttpResponseOutput, HttpResponse}
      import hector.http.conversion._

      // Create a Hector HttpRequest form a given ServletRequest

      val request = HttpRequestConversion.fromHttpServletRequest(httpRequest)

      // We handle the request by asking the router for
      // the appropriate actor and message we have to
      // dispatch.

      val route: Future[Option[Route[Any]]] = ask(router, request)(Hector.config.defaultRouteTimeout).mapTo[Option[Route[Any]]]

      // The optional response we generate. Note that there is nothing written yet and this
      // is only a container for the actual response.

      val response: Future[Option[HttpResponse]] =
        (route flatMap {
          case Some(Route(actor, arguments, timeout, strategy)) ⇒
            ask(actor, CreateResponse(request, arguments))(timeout) map {
              case response: HttpResponse ⇒
                letItCrash()
                response
              case other ⇒
                letItCrash()
                createTypeErrorResponse(actor, other)
            } recover (strategy getOrElse defaultRecoverStrategy(actor, timeout)) map { Some(_) }

          case None ⇒
            // We are not participating in this request. Let someone
            // else handle this case.

            Promise.successful(None)
        }).mapTo[Option[HttpResponse]]

      // In this step we complete the response. It is Some in case we participated in the request
      // or None if Hector did not touch the request because there was no route.
      //
      // This future does not have to complete very quickly and it is very likely that
      // value.writeContent takes a long time to complete or "never" completes in case of
      // a long-polling scenario.

      val output: Future[Option[Unit]] =
        response flatMap {
          case Some(nullThing) if nullThing == null ⇒
            log.error("The request {} lead to a null-response.", request)
            throw new RuntimeException("Null-response for "+request+".")

          case Some(value) ⇒
            import hector.http.status.NoContent

            letItCrash()

            if(value.status == NoContent) {
              // This is a workaround for Jetty which is quite annoying. If we do not flush
              // the buffer or create an output stream the NoContent header is replaced with
              // 200. If we omit the flushBuffer() the servlet container creates a 404.

              fillServletResponse(value, httpResponse, request.session)
              httpResponse.flushBuffer()

              Promise.successful(Some(()))
            } else {
              val outputStream = httpResponse.getOutputStream
              val responseOutput =
                new OutputStreamHttpResponseOutput(
                  encoding = value.characterEncoding getOrElse Hector.config.defaultCharset,
                  output = outputStream
                )

              fillServletResponse(value, httpResponse, request.session)

              letItCrash()

              // Write the body to the output stream

              import java.io.{EOFException ⇒ JEOFException}

              value.writeContent(responseOutput) recover {
                case eofException: JEOFException ⇒
                  // In case of a premature end of file because the client closed the
                  // connection we are not interested in what happens.
                  ()
              } map { Some(_) }
            }

          case None ⇒
            // Nothing to do for us.

            Promise.successful(None)
        } recover {
          case throwable: Throwable ⇒
            Hector.statistics ! ExceptionOccurred(throwable)

            log.error(throwable, "Exception occurred while serving {}.", request)

            if(httpResponse.isCommitted) {
              log.warning("HttpResponse is already comitted. Cannot do anything about this.")
            } else {
              import java.io.{PrintStream ⇒ JPrintStream}

              httpResponse.setStatus(500)
              httpResponse.setHeader("X-Powered-By", "Hector")
              httpResponse.setContentType("text/plain")

              val output = httpResponse.getOutputStream
              val printStream = new JPrintStream(output)

              if(Hector.config.runMode < RunModes.Production) {
                //TODO(joa): proper html
                throwable.printStackTrace(printStream)
              } else {
                printStream.print("ERROR")
              }

              printStream.flush()
              output.flush()
            }

            Some(())
        }

      output pipeTo sender
  }

  private def fillServletResponse(source: HttpResponse,  target: HttpServletResponse, session: Option[HttpSession]) {
    import hector.http.conversion._
    import hector.http.status.NoContent

    // Send our headers and cookies

    source.contentLength foreach target.setContentLength
    source.cookies map HttpCookieConversion.toServletCookie foreach target.addCookie
    source.headers foreach { header ⇒ target.setHeader(header.name, header.value) }

    session foreach {
      value ⇒
        import javax.servlet.http.Cookie

        val sessionCookie = new Cookie(Hector.config.sessionCookieName, value.id)

        sessionCookie.setMaxAge(Hector.config.sessionLifetime.toSeconds.toInt)
        sessionCookie.setPath("/")
        sessionCookie.setHttpOnly(Hector.config.sessionCookieHttpOnly)
        sessionCookie.setSecure(Hector.config.sessionCookieSecure)

        target.addCookie(sessionCookie)
    }
    
    letItCrash()

    if(source.status != NoContent) {
      target.setContentType(source.contentType)
    }

    target.setStatus(source.status)
    target.setHeader("X-Powered-By", "Hector")
  }

  private def createTypeErrorResponse(actor: ActorRef,  value: Any) = {
    import hector.http.HtmlResponse
    import hector.http.status.InternalServerError
    import hector.html.DocTypes

    HtmlResponse(
      <html>
        <head>
          <title>Type Error</title>
        </head>
        <body>
          <h1>Type Error</h1>
          <p>The actor <strong>{actor.toString()}</strong> did not respond with an HttpResponse object.</p>
          <p>Instead it responded with: {value}</p>
        </body>
      </html>,
      DocTypes.`HTML 5`,
      InternalServerError
    )
  }

  private def defaultRecoverStrategy(actor: ActorRef, timeout: Timeout): PartialFunction[Throwable, HttpResponse] = {
    case askTimeoutException: AskTimeoutException ⇒
      import hector.http.PlainTextResponse
      Hector.statistics ! ExceptionOccurred(askTimeoutException)
      log.error("Actor "+actor+" did not respond within "+timeout.duration+".")

      //TODO(joa): proper html
      PlainTextResponse(
        if(Hector.config.runMode < RunModes.Production) {
          "Actor "+actor+" did not respond within "+timeout.duration+"."
        } else {
          "ERROR"
        }, status = 500)

    case throwable: Throwable ⇒
      import hector.http.PlainTextResponse
      log.error(throwable, "Exception occurred in "+actor+".")

      //TODO(joa): proper html
      PlainTextResponse(
        if(Hector.config.runMode < RunModes.Production) {
          throwable.getMessage
        } else {
          "ERROR"
        }, status = 500)
  }
}
