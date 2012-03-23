package hector.actor

import akka.actor.{ActorRef, Props, Actor}
import akka.dispatch._
import akka.routing.RoundRobinRouter
import akka.routing.DefaultResizer
import akka.util.Timeout
import akka.util.duration._

import javax.servlet.AsyncContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import hector.Hector
import hector.util.letItCrash
import akka.pattern.{AskTimeoutException, ask}
import hector.http.{HttpStatus, HttpCookieConversion, HttpResponse}

object RequestActor {
  sealed trait RootMessage
  final case class HandleAsync(asyncContext: AsyncContext) extends RootMessage
  final case class HandleRequest(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse) extends RootMessage
}

/**
 * @author Joa Ebert
 */
final class RequestActor extends Actor {
  import RequestActor._

  private[this] val router =
      context.actorOf(
        Props[RouterActor].
          withRouter(
            RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))))

  //TODO(joa): Default request timeout (needs to be configurable)
  private[this] implicit val implicitTimeout = Timeout(60.seconds)

  override protected def receive = {
    case HandleAsync(asyncContext) ⇒
      import akka.pattern.pipe
      import akka.pattern.AskTimeoutException
      import StatisticsActor.RequestCompleted

      letItCrash()

      val t0 = System.nanoTime()

      val request =
        self ? HandleRequest(
          asyncContext.getRequest.asInstanceOf[HttpServletRequest],
          asyncContext.getResponse.asInstanceOf[HttpServletResponse]
        )

      request onComplete {
        case Right(Some(_)) ⇒
          Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)

        case Right(None) ⇒
          // Nothing to do. We did not participate.

        case Left(askTimeout: AskTimeoutException) ⇒
          Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)
          println("[ERROR]: Timeout. Did you forget to reply in your actor?") //TODO(joa): can we get the actor info somehow?

        case Left(error) ⇒
          //TODO(joa): do something useful.
          Hector.statistics ! RequestCompleted((System.nanoTime() - t0).toFloat * 0.000001f)
          println("Error: "+error.getMessage)
          error.printStackTrace()
      }

      request pipeTo sender

    case HandleRequest(httpRequest, httpResponse) ⇒
      import akka.pattern.pipe
      import com.google.common.base.Charsets
      import context.dispatcher
      import hector.actor.RouterActor.Route
      import hector.http.{HttpCookieConversion, OutputStreamHttpResponseOutput, HttpResponse, HttpRequestConversion, HttpStatus}

      // Create a Hector HttpRequest form a given ServletRequest

      val request = HttpRequestConversion.fromHttpServletRequest(httpRequest)

      // We handle the request by asking the router for
      // the appropriate actor and message we have to
      // dispatch.

      val route: Future[Option[Route[Any]]] = (router ? request).mapTo[Option[Route[Any]]]

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
          case Some(value) ⇒
            letItCrash()

            if(value.status == HttpStatus.NoContent) {
              // This is a workaround for Jetty which is quite annoying. If we do not flush
              // the buffer or create an output stream the NoContent header is replaced with
              // 200. If we omit the flushBuffer() the servlet container creates a 404.

              fillServletResponse(value, httpResponse)
              httpResponse.flushBuffer()

              Promise.successful(Some(()))
            } else {
              val outputStream = httpResponse.getOutputStream
              val responseOutput =
                new OutputStreamHttpResponseOutput(
                  encoding = value.characterEncoding getOrElse Charsets.UTF_8,  //TODO(joa): UTF-8 must be configurable
                  output = outputStream
                )

              fillServletResponse(value, httpResponse)

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
            //TODO(joa): proper html, stats

            println("[ERROR]: "+throwable.getMessage)
            throwable.printStackTrace()

            if(httpResponse.isCommitted) {
              println("HttpResponse is already comitted. Cannot do anything about this.")
            } else {
              import java.io.{PrintStream ⇒ JPrintStream}

              httpResponse.setStatus(500)
              httpResponse.setHeader("X-Powered-By", "Hector")
              httpResponse.setContentType("text/plain")

              val output = httpResponse.getOutputStream
              val printStream = new JPrintStream(output)

              throwable.printStackTrace(printStream)

              printStream.flush()
              output.flush()
            }

            Some(())
        }

      output pipeTo sender
  }

  private def fillServletResponse(source: HttpResponse,  target: HttpServletResponse) {
    // Send our headers and cookies

    source.contentLength foreach target.setContentLength
    source.cookies map HttpCookieConversion.toServletCookie foreach target.addCookie
    source.headers foreach { header ⇒ target.setHeader(header.name, header.value) }

    letItCrash()

    if(source.status != HttpStatus.NoContent) {
      target.setContentType(source.contentType)
    }

    target.setStatus(source.status)
    target.setHeader("X-Powered-By", "Hector")
  }

  private def createTypeErrorResponse(actor: ActorRef,  value: Any) = {
    import hector.http.HtmlResponse
    import hector.http.HttpStatus
    import hector.html.DocType

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
      DocType.`HTML 5`,
      HttpStatus.InternalServerError
    )
  }

  private def defaultRecoverStrategy(actor: ActorRef, timeout: Timeout): PartialFunction[Throwable, HttpResponse] = {
    case askTimeoutException: AskTimeoutException ⇒
      import hector.http.PlainTextResponse
      //TODO(joa): proper html, stats
      println("[ERROR]: Actor "+actor+" did not respond within "+timeout.duration+".")
      PlainTextResponse("Actor "+actor+" did not respond within "+timeout.duration+".", status = 500)
    case throwable: Throwable ⇒
      import hector.http.PlainTextResponse
      //TODO(joa): proper html, stats
      println("[ERROR]: "+throwable.getMessage)
      println(throwable)
      PlainTextResponse(throwable.getMessage, status = 500)
  }
}
