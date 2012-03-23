package hector.http

import javax.servlet.http.HttpServletRequest

/**
 * @author Joa Ebert
 */
object HttpMethod {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest) =
    fromString(httpServletRequest.getMethod)

  def fromString(method: String) =
    Symbol(method.toUpperCase) match {
      case 'OPTIONS ⇒ Options
      case 'GET ⇒ Get
      case 'HEAD ⇒ Head
      case 'POST ⇒ Post
      case 'PUT ⇒ Put
      case 'DELETE ⇒ Delete
      case 'TRACE ⇒ Trace
      case 'CONNECT ⇒ Connect
    }
}

sealed trait HttpMethod extends Serializable

case object Options extends HttpMethod
case object Get extends HttpMethod
case object Head extends HttpMethod
case object Post extends HttpMethod
case object Put extends HttpMethod
case object Delete extends HttpMethod
case object Trace extends HttpMethod
case object Connect extends HttpMethod
