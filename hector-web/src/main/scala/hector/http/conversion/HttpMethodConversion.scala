package hector.http.conversion

import hector.http.HttpMethods._

import javax.servlet.http.HttpServletRequest

object HttpMethodConversion {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest) =
    fromString(httpServletRequest.getMethod)

  def fromString(method: String) =
    method.toLowerCase match {
      case "options" ⇒ Options
      case "get" ⇒ Get
      case "head" ⇒ Head
      case "post" ⇒ Post
      case "put" ⇒ Put
      case "delete" ⇒ Delete
      case "trace" ⇒ Trace
      case "connect" ⇒ Connect
    }
}
