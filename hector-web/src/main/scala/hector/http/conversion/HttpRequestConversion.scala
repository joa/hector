package hector.http.conversion

import hector.http._

import javax.servlet.http.HttpServletRequest

object HttpRequestConversion {
  private final class HttpServletRequestWrapper(httpServletRequest: HttpServletRequest) extends HttpRequest {
    val method = HttpMethodConversion fromHttpServletRequest httpServletRequest

    val path = HttpPathConversion fromHttpServletRequest httpServletRequest

    val cookies = HttpCookieConversion fromHttpServletRequest httpServletRequest

    val headers = HttpHeaderConversion fromHttpServletRequest httpServletRequest

    val authType = HttpAuthTypeConversion fromHttpServletRequest httpServletRequest
  }

  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): HttpRequest =
    new HttpServletRequestWrapper(httpServletRequest)
}


