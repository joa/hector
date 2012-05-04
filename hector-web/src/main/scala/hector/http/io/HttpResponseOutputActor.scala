package hector.http.io

import akka.actor.Actor

import java.nio.charset.{Charset ⇒ JCharset}
import java.io.{OutputStream ⇒ JOutputStream}

/**
 */
final class HttpResponseOutputActor(
    private[this] val encoding: JCharset,
    private[this] val output: JOutputStream) extends Actor {
  override protected def receive = {
    case Write(data, offset, length) ⇒ output.write(data.backingArray, offset, length)
    case Flush ⇒ output.flush()
    case value: String ⇒ print(value)
    case value: Byte ⇒ print(String.valueOf(value))
    case value: Int ⇒ print(String.valueOf(value))
    case value: Long ⇒ print(String.valueOf(value))
    case value: Float ⇒ print(String.valueOf(value))
    case value: Double ⇒ print(String.valueOf(value))
    case value: Char ⇒ print(String.valueOf(value))
  }

  private[this] def print(value: String) {
    output.write(value.getBytes(encoding))
  }
}
