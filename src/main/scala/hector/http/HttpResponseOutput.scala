package hector.http

import java.nio.charset.{Charset ⇒ JCharset}
import java.io.{OutputStream ⇒ JOutputStream}

/**
 * The HttpResponseOutput trait defines methods to generate the actual body of a HttpResponse.
 *
 * @author Joa Ebert
 */
trait HttpResponseOutput {
  def print(value: Byte)

  def print(value: Int)

  def print(value: Long)

  def print(value: Float)

  def print(value: Double)

  def print(value: Char)

  def print(value: String)

  def write(value: Int)

  def write(bytes: Array[Byte], offset: Int, length: Int)

  def flush()

  def println(value: Byte) {
    print(value)
    print('\n')
  }

  def println(value: Int) {
    print(value)
    print('\n')
  }

  def println(value: Long) {
    print(value)
    print('\n')
  }

  def println(value: Float) {
    print(value)
    print('\n')
  }

  def println(value: Double) {
    print(value)
    print('\n')
  }

  def println(value: Char) {
    print(value)
    print('\n')
  }

  def println(value: String) {
    print(value)
    print('\n')
  }

  def write(bytes: Array[Byte]) {
    write(bytes, 0, bytes.length)
  }
}

/**
 * The OutputStreamHttpResponseOutput class is a wrapper for Java's OutputStream.
 *
 * @param encoding The encoding of the HttpResponseOutput.
 * @param output The Java OutputStream to wrap.
 *
 * @author Joa Ebert
 */
final class OutputStreamHttpResponseOutput(
    private[this] val encoding: JCharset,
    private[this] val output: JOutputStream) extends HttpResponseOutput {
  override def print(value: Byte) {
    print(String.valueOf(value))
  }

  override def print(value: Int) {
    print(String.valueOf(value))
  }

  override def print(value: Long) {
    print(String.valueOf(value))
  }

  override def print(value: Float) {
    print(String.valueOf(value))
  }

  override def print(value: Double) {
    print(String.valueOf(value))
  }

  override def print(value: Char) {
    print(String.valueOf(value))
  }

  override def print(value: String) {
    write(value.getBytes(encoding))
  }

  override def write(value: Int) {
    output.write(value)
  }

  override def write(bytes: Array[Byte], offset: Int, length: Int) {
    output.write(bytes, offset, length)
  }

  override def flush() {
    output.flush()
  }
}
