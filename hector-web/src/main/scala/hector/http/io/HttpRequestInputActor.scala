package hector.http.io

import akka.actor.Actor

import java.nio.charset.{Charset ⇒ JCharset}
import java.io.{InputStream ⇒ JInputStream}

/**
 */
final class HttpRequestInputActor(
    private[this] val encoding: JCharset,
    private[this] val input: JInputStream) extends Actor {
  override protected def receive = {
    case _ ⇒
  }
}
