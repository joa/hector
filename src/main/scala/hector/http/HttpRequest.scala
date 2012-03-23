package hector.http

import javax.servlet.http.HttpServletRequest

object HttpRequestConversion {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): HttpRequest = {
    HttpRequest(
      method = HttpMethod fromHttpServletRequest httpServletRequest,
      path = HttpPath fromHttpServletRequest httpServletRequest
    )
  }
}

/**
 * @author Joa Ebert
 */
final case class HttpRequest(method: HttpMethod, path: HttpPath) {
  //TODO(joa): params, cookies, headers, ...
}


