package hector.http.conversion

import com.google.common.base.{Splitter ⇒ GSplitter}

import hector.http._

import javax.servlet.http.HttpServletRequest
import java.util.{Iterator ⇒ JIterator}

object HttpPathConversion {
  private[this] val slashSplitter = GSplitter.on('/')

  def fromHttpServletRequest(httpRequest: HttpServletRequest) =
    fromString(httpRequest.getRequestURI)

  def fromString(value: String) = {
    if(value == null) {
      No_/
    } else {
      iteratorToPath(
        iterator = slashSplitter.split(value).iterator(),
        endsWithSlash = value endsWith "/")
    }
  }

  //TODO(joa): get rid of recursion
  private[this] def iteratorToPath(iterator: JIterator[String], endsWithSlash: Boolean): HttpPath =
    iterator.hasNext match {
      case true ⇒
        var head = ""

        do {
          head = iterator.next()
        } while(head == "" && iterator.hasNext)

        if(head == "") {
          endsWithSlash match {
            case true ⇒ Required_/
            case false ⇒ No_/
          }
        } else {
          new /:(head, iteratorToPath(iterator, endsWithSlash))
        }

      case false ⇒
        endsWithSlash match {
          case true ⇒ Required_/
          case false ⇒ No_/
        }
    }
}
