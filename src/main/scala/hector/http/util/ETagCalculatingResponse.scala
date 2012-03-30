package hector.http.util

import akka.dispatch.{Future, ExecutionContext}
import akka.dispatch.Await
import akka.util.duration._

import com.google.common.base.Charsets
import com.google.common.io.Closeables
import com.google.common.hash.Hashing

import hector.http.{OutputStreamHttpResponseOutput, HttpResponseOutput, HttpResponse}
import hector.http.header.ETag

import java.io.ByteArrayOutputStream
import javax.annotation.Nullable

import hector.Hector

/**
 * The ETagCalculatingResponse class appends an ETag header to a given HttpResponse.
 *
 * Creating an ETag header automatically comes with a performance penalty.
 * Asking for the <code>headers</code> will cause the HttpResponse to be serialized
 * into an array of bytes so a hash-code can be calculated. Serialization is performed
 * blocking. This means that
 */
final case class ETagCalculatingResponse(response: HttpResponse) extends HttpResponse {
  @transient @volatile @Nullable
  private[this] var data: Array[Byte] = null

  def status = response.status

  def cookies = response.cookies

  def headers = {
    //TODO(joa): use different ctx
    implicit val executor: ExecutionContext = Hector.system.dispatcher
    response.headers :+ createETag()
  }

  def contentType = response.contentType

  def characterEncoding = response.characterEncoding

  def contentLength = response.contentLength

  def writeContent(output: HttpResponseOutput)(implicit executor: ExecutionContext) =
    if(null == data) {
      // No-one asked for the headers. In that case we skip the temporary serialization.
      //
      // Note: Of course this is not thread-safe and a race condition. Data could in be
      //       computed while we start writing it but  if that is the case we do not care
      //       and it is written twice. It is written once to the byte array in order to
      //       be hashed, and it is written once to the actual response.

      response.writeContent(output)
    } else {
      // serializeData() has been called before. Therefore we simply pass the data we already
      // know into the output stream.

      Future {
        output.write(data)
      }
    }

  private[this] def createETag()(implicit executor: ExecutionContext): ETag = {
    val hashCode = Hashing.murmur3_128.hashBytes(serializedData())
    ETag(hashCode.toString)
  }

  private[this] def serializedData()(implicit executor: ExecutionContext): Array[Byte] =
    if(null == data) {
      val byteArrayOutputStream = new ByteArrayOutputStream(contentLength getOrElse 0x100)

      try {
        val future =
          response.writeContent(new OutputStreamHttpResponseOutput(characterEncoding getOrElse Charsets.UTF_8, byteArrayOutputStream))

        //TODO(joa): what about this timeout?
        Await.result(future, 1.second)

        byteArrayOutputStream.flush()

        data = byteArrayOutputStream.toByteArray
        data
      } finally {
        Closeables.closeQuietly(byteArrayOutputStream)
      }
    } else {
      data
    }
}
