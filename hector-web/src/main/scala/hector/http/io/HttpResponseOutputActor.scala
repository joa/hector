package hector.http.io

import akka.actor.Actor

import java.nio.charset.{Charset ⇒ JCharset}
import java.io.{OutputStream ⇒ JOutputStream}

/**
 */
final class HttpResponseOutputActor(
    private[this] val encoding: JCharset,
    private[this] val output: JOutputStream) extends Actor {
  override def receive = {
    case Write(data, offset, length) ⇒
      output.write(data.backingArray, offset, length)
      ack()

    case Flush ⇒
      output.flush()
      ack()

    case value: String ⇒
      print(value)
      ack()

    case value: Byte ⇒
      print(String.valueOf(value))
      ack()

    case value: Int ⇒
      print(String.valueOf(value))
      ack()

    case value: Long ⇒
      print(String.valueOf(value))
      ack()

    case value: Float ⇒
      print(String.valueOf(value))
      ack()

    case value: Double ⇒
      print(String.valueOf(value))
      ack()

    case value: Char ⇒
      print(String.valueOf(value))
      ack()
  }

  private[this] def ack() {
    if(sender != null) {
      sender ! Ack
    }
  }

  private[this] def print(value: String) {
    output.write(value.getBytes(encoding))
  }
}
