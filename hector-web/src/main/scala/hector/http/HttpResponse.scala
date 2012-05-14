package hector.http

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask

import hector.html.{DocType, DocTypes}
import hector.html.emitter.HtmlEmitter
import hector.http.header.{Connection, CacheControl}
import hector.http.io.Flush
import hector.js.emitter.JsEmitter
import hector.util.MimeType

import java.nio.charset.{Charset ⇒ JCharset}

import scala.xml.Node
import hector.js.{JsObj, JsAST}
import akka.dispatch.{Promise, Future, ExecutionContext}

/**
 */
trait HttpResponse extends Serializable {
  protected implicit def defaultTimeout: Timeout = {
    import akka.util.duration._
    Timeout(5.seconds)
  }

  /**
   * The status code of the response.
   *
   * @return A Http status code with 200 indicating a plain OK result.
   */
  def status: Int

  /**
   * The cookies associated with the response.
   *
   * <p>Each cookie will be sent to the client via the Set-Cookie header.</p>
   *
   * @return A sequence of cookies.
   */
  def cookies: Seq[HttpCookie]

  /**
   * The headers associated with the response.
   *
   * <p>Some headers like Set-Cookie will be inserted automatically.</p>
   *
   * @return A list of headers.
   */
  def headers: Seq[HttpHeader]

  /**
   * The content type of the response.
   *
   * @return A valid content type.
   */
  def contentType: String

  /**
   * The encoding used for the response.
   *
   * <p>The global default encoding will be used if not specified.</p>
   *
   * @return <code>Some</code> encoding if necessary; <code>None</code> otherwise.
   */
  def characterEncoding: Option[JCharset]

  /**
   * Computes and returns the length of the content.
   *
   * @return <code>Some</code> value in case the length is known and can be computed; <code>None</code> otherwise.
   */
  def contentLength: Option[Int]

  /**
   * Writes the content of this response to a HttpResponseOutput. */
  def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_]
}

final case class HtmlResponse(html: Node, docType: DocType = DocTypes.`HTML 5`, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None) extends HttpResponse {
  @transient private[this] lazy val htmlAsString = HtmlEmitter.toString(html, docType, stripComments = false, trim = true, humanReadable = false)

  override def contentType = MimeType.text.html

  override def contentLength = Some(htmlAsString.length)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    output ! htmlAsString
    output ? Flush
  }
}

final case class EmptyResponse(status: Int = 204, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None) extends HttpResponse {
  override def contentType = MimeType.text.plain

  override def contentLength = Some(0)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = { Promise.successful(null) }

}

final case class XMLResponse(xml: Node, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None) extends HttpResponse {
  @transient private[this] lazy val xmlAsString = xml.toString()

  override def contentType = MimeType.text.xml

  override def contentLength = Some(xmlAsString.length)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    output ! xmlAsString
    output ? Flush
  }
}

final case class JsResponse(js: JsAST, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None, humanReadable: Boolean = false) extends HttpResponse {
  @transient private[this] lazy val jsAsString = JsEmitter.toString(js, humanReadable)

  override def contentType = MimeType.application.javascript

  override def contentLength = Some(jsAsString.length)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    output ! jsAsString
    output ? Flush
  }
}

final case class JsonResponse(json: JsObj, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None, humanReadable: Boolean = false) extends HttpResponse {
  @transient private[this] lazy val jsAsString = JsEmitter.toString(json, humanReadable)

  override def contentType = MimeType.application.json

  override def contentLength = Some(jsAsString.length)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    output ! jsAsString
    output ? Flush
  }
}

final case class PlainTextResponse(text: String, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq.empty, characterEncoding: Option[JCharset] = None) extends HttpResponse {
  override def contentType = MimeType.text.plain

  override def contentLength = Some(text.length)

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    output ! text
    output ? Flush
  }
}

final case class EventStreamResponse(target: ActorRef, timeout: Timeout, status: Int = 200, cookies: Seq[HttpCookie] = Seq.empty, headers: Seq[HttpHeader] = Seq(CacheControl("no-cache"), Connection("keep-alive")), characterEncoding: Option[JCharset] = None) extends HttpResponse {
  override def contentType = MimeType.text.eventStream

  override def contentLength = None

  override def writeContent(output: ActorRef)(implicit executionContext: ExecutionContext): Future[_] = {
    import akka.pattern.ask

    ask(target, output)(timeout).mapTo[Unit] recover {
      case _ ⇒
        // In case the target had no time to complete we perform this action here. This is okay
        // because the client should reconnect to an EventSource and we expect that the timeout
        // can be exceeded sometimes due to inaccuracy.

        ()
    }
  }
}
