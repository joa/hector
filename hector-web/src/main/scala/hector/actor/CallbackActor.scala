package hector.actor

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.dispatch.Promise
import akka.pattern.pipe
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask

import hector.Hector
import hector.js.JsAST
import hector.util.{randomHash, isAlphaNumeric}
import hector.config.RunModes
import hector.http.{EmptyResponse, HttpRequest, HttpResponse}

/**
 */
object CallbackActor {
  sealed trait CallbackActorMessage

  /**
   * The message to create a new callback.
   *
   * <p>The callback Actor will respond with a <code>Future[JsAST]</code>.</p>
   *
   * @param request The current HTTP request.
   * @param target The target actor to be notified.
   * @param message The message to send to the target actor.
   */
  case class NewCallback(request: HttpRequest, target: ActorRef, message: Any) extends CallbackActorMessage

  /**
   * The message to execute a callback.
   *
   * <p>Hector is dispatching this message internally it it is not necessary to dispatch
   * it manually.</p>
   *
   * @param callback The callback to execute.
   */
  case class Execute(callback: String) extends CallbackActorMessage

  /**
   * The length of a callback name.
   */
  val CallbackNameLength = 32
}

/**
 * The CallbackActor can be used to create a callback which can be triggered via client-side code.
 *
 * <p>User-code must embed <code>Hector.clientSupport</code> into the web page in order to make
 * use of callbacks. If it is not desired to use the Hector client support library User-code must
 * define the JavaScript function <code>hector.executeCallback(name)</code>.</p>
 *
 * <p>A callback in Hector is defined as a message which is dispatched to an actor. The life-cycle
 * of a callback is as follows:</p>
 *
 * <p>
 *   <ol>
 *     <li>User-code creates a new callback by dispatching <code>NewCallback</code> to
 *       <code>Hector.callback</code>.</li>
 *     <li>Hector saves the parameters in the session and responds with the necessary JavaScript
 *       code to trigger the callback on the server side.</li>
 *     <li>User-code sends the JavaScript to the client in a web page for example.</li>
 *     <li>The client executes the JavaScript which loads a special URL.</li>
 *     <li>Hector receives the requests and routes it to the CallbackActor.</li>
 *     <li>The callback actor tries to lookup the parameters from the session and dispatches the
 *       message to the specified target.</li>
 *     <li>The target actor (User-code) should respond with an <code>HttpResponse</code> object.
 *       Otherwise automatic response conversion applies.</li>
 *     <li>The response is sent back to the client.</li>
 *     <li>The client will execute any given JavaScript or append any given XML to the body of the
 *       document.</li>
 *   </ol>
 * </p>
 *
 * <p>Automatic response conversion happens when User-code does not reply with an HttpResponse object.
 * The rules for the conversion process are:</p>
 *
 * <p>
 *   <ol>
 *     <li>A sub-type of <code>scala.xml.Node</code> will be wrapped in <code>XMLResponse</code>
 *       with <code>HttpStatus.Ok</code>.</li>
 *     <li>A sub-type of <code>hector.js.JsAST</code> will be wrapped in <code>JsResponse</code>
 *       with <code>HttpStatus.Ok</code>.</li>
 *     <li>A sub-type of <code>scala.Seq[_]</code> will be wrapped in <code>JsResponse</code> with
 *       <code>HttpStatus.Ok</code> if all of its elements are a sub-type of JsAST.
 *       Each AST element will be converted to a JsExpStatement if it is an JsExpression.
 *       The sequence of JsStatement objects will be wrapped in a JsProgram.</li>
 *     <li>A string will be wrapped in a <code>PlainTextResponse</code> with
 *       <code>HttpStatus.Ok</code>.</li>
 *     <li>Other objects will not be converted to a response automatically. Instead in development
 *       mode a JsResponse is generated and will notify the developer about the actor responsible
 *       for generating this response. Hector will only log a warning message in production code and
 *       create an empty response with <code>HttpStatus.Accepted</code>.
 *   </ol>
 * </p>
 *
 * A complete example:
 *
 * {{{
 *   //
 *   // User code which generates a website.
 *   //
 *
 *   val callbackFuture = Hector.callback ? NewCallback(request, targetActor, "Execute")
 *   ...
 *
 *
 *   val result =
 *     for {
 *       callback ← callbackFuture.mapTo[JsAST]
 *     } yield {
 *       HtmlResponse(
 *         <html>
 *           <head>
 *             <title>Callback Example</title>
 *             {Hector.clientSupport}
 *           </head>
 *           <body>
 *             <a href="#" onclick={callback.emit()}>Execute</a>
 *           </body>
 *         </html>,
 *         DocType.`HTML 5`)
 *     }
 *
 *     result pipeTo sender
 *
 *   //
 *   // User-code which defines the target actor.
 *   //
 *
 *   override def receive = {
 *     case "Execute" ⇒
 *       import hector.js.implicits._
 *       import hector.js.toplevel.{jsWindow ⇒ window}
 *
 *       sender ! window.alert("Hello World.")
 *   }
 * }}}
 */
final class CallbackActor extends Actor with ActorLogging {
  import CallbackActor._

  private[this] implicit val implicitDispatcher = context.dispatcher

  override protected def receive = {
    case NewCallback(request, target, message) ⇒ newCallback(request, target, message)

    case CreateResponse(request, Some(Execute(callbackName))) ⇒ executeCallback(request, callbackName)
  }

  /**
   * Creates and returns the session key for the given callback.
   *
   * @param callbackName The name of the callback.
   *
   * @return The session key.
   */
  private[this] def createSessionHash(callbackName: String): String =
    "hector:callback:"+callbackName

  /**
   * Creates and returns an appropriate JavaScript AST which will execute the
   * callback.
   *
   * <p>The generated JavaScript code is in the form <code>(function() { hector.execCallback(name); })();</code>
   * and can be embedded in a script or in an action handler like <code>onclick</code> of an anchor tag.</p>
   *
   * @param callbackName The name of the callback.
   *
   * @return The JavaScript code to execute the callback.
   */
  private[this] def createJavaScriptCall(callbackName: String): JsAST = {
    import hector.js._
    import implicits._

    JsCall(
      callee = JsFunc(
        id = None,
        parameters = Seq.empty,
        body = JsBlock(('hector ~> 'execCallback)(callbackName))
      ),
      arguments = Seq.empty
    )
  }

  /**
   * Creates and returns a new callback.
   *
   * <p>Creating a callback is done via the following steps:
   *   <ol>
   *     <li>Create a new random hash via the <code>UtilityActor</code>.</li>
   *     <li>Store the target actor and message with the request session.</li>
   *     <li>Create and return the JavaScript code necessary to trigger the callback.</li>
   *   </ol>
   * </p>
   *
   * @param request The client's HttpRequest.
   * @param target The target actor.
   * @param message The message to send to the actor.
   *
   * @return The JavaScript code to execute the callback.
   */
  private[this] def newCallback(request: HttpRequest, target: ActorRef, message: Any) = {
    val callbackName = randomHash()
    val storeFuture = Hector.sessionStore(request, createSessionHash(callbackName), (target, message))

    val jsFunctionFuture =
      storeFuture map { unit ⇒ createJavaScriptCall(callbackName) }

    jsFunctionFuture pipeTo sender
  }

  private[this] def isInvalidCallbackName(name: String): Boolean =
    name.length != CallbackNameLength || !isAlphaNumeric(name)

  /**
   * Executes a callback returns the result.
   *
   * <p>Executing a callback is done via the following steps:
   *   <ol>
   *     <li>Validate the hash for integrity. Respond with an error if the hash is not valid.</li>
   *     <li>Ask the session storage for the target actor and message associated with the callback.
   *       Respond with NotFound if no such callback exists.</li>
   *     <li>Send the message to the target actor and await its response.</li>
   *     <li>Convert the response to an HttpResponse if possible.</li>
   *   </ol>
   * </p>
   *
   * @param request The client's HttpRequest.
   * @param callbackName The name of the callback.
   *
   * @return The result of executing the callback.
   */
  private[this] def executeCallback(request: HttpRequest, callbackName: String) = {
    import hector.http.PlainTextResponse
    import hector.http.status.{BadRequest, NotFound}

    if(isInvalidCallbackName(callbackName)) {
      // The user provided an illegal callback name.
      sender ! PlainTextResponse("Invalid callback.\n", BadRequest)
    } else {
      val sessionFuture =
        Hector.sessionLoad[Option[(ActorRef, Any)]](request, createSessionHash(callbackName))

      sessionFuture flatMap {
        sessionValue ⇒
          sessionValue match {
            case Some((actor, message)) ⇒
              (ask(actor, message)(Timeout(10.seconds))) map toResponse(actor) //TODO(joa): timeout should be configurable

            case None ⇒
              Promise.successful(
                PlainTextResponse(
                  text = "No such callback.\n",
                  status = NotFound
                )
              )
          }
      } pipeTo sender
    }
  }

  private[this] def toResponse(actor: ActorRef)(value: Any): HttpResponse = {
    import hector.http.{XMLResponse, JsResponse, PlainTextResponse}
    import hector.js.JsAST

    import scala.xml.Node

    value match {

      // We perform automatic response conversion here.

      case response: HttpResponse ⇒
        // If we have been provided with a response already we just need to
        // forward it.
        response

      case node: Node ⇒
        // It is debatable if scala.xml.Node should be returned as XML or HTML
        // but for the sake of simplicity we treat it as normal XML data since
        // the client-side scripting will pick it up as an XML literal.
        XMLResponse(node)

      case ast: JsAST ⇒
        // A JavaScript syntax tree will be sent as application/javascript.
        JsResponse(ast)

      case seq: Seq[_] if seq forall { _.isInstanceOf[JsAST] } ⇒
        // Convert a sequence of JavaScript into a JsProgram and send it to
        // the client.
        import hector.js.{JsProgram, JsStatement,  JsExpression, JsExpStatement}

        JsResponse(
          js = JsProgram(
            seq map {
              case statement: JsStatement ⇒ statement
              case expression: JsExpression ⇒ JsExpStatement(expression)
            }
          )
        )

      case text: String ⇒
        PlainTextResponse(text)

      case other ⇒
        // It is not possible for us to process the content automatically. Since
        // this could be dangerous we will only tell the developer during development
        // what he did wrong in order to resolve the conflict.

        import hector.http.status.Accepted

        if(Hector.config.runMode < RunModes.Production) {
          import hector.js.implicits._
          import hector.js.toplevel.{jsWindow ⇒ window}

          JsResponse(
            js = window.alert("Error: Cannot convert "+other+" to a response.\nThe actor "+actor+" is responsible.\nVisit TODO for more help."),
            status = Accepted
          )
        } else {
          log.warning("Cannot convert {} to a response. The actor {} is responsible for this missbehaviour.", other, actor)
          EmptyResponse(status = Accepted)
        }
    }
  }
}
