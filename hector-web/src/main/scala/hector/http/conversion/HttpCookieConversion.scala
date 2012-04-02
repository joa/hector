package hector.http.conversion

import hector.http._
import hector.util.trimToOption

import javax.servlet.http.{HttpServletRequest, Cookie}

/**
 *
 */
object HttpCookieConversion {
  /**
   * Converts a Hector cookie to a Servlet cookie representation.
   *
   * @param cookie The Hector cookie representation.
   * @return The servlet cookie representation.
   */
  def toServletCookie(cookie: HttpCookie): Cookie = {
    val result = new Cookie(cookie.name, cookie.value)

    cookie.maxAge foreach result.setMaxAge
    cookie.path map { _.toString } foreach result.setPath
    cookie.domain foreach result.setDomain
    cookie.httpOnly foreach result.setHttpOnly
    cookie.secure foreach result.setSecure

    result
  }

  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): Seq[HttpCookie] = {
    val cookieArray = httpServletRequest.getCookies

    val n = cookieArray.length
    var i = 0

    var result = Vector.empty[HttpCookie]

    while(i < n) {
      result = result :+ fromServletCookie(cookieArray(i))

      i += 1
    }

    result
  }

  def fromServletCookie(cookie: Cookie): HttpCookie =
    HttpCookie(
      name = cookie.getName,
      value = cookie.getValue,
      maxAge = cookie.getMaxAge match {
        case x if x < 0 ⇒ None
        case x ⇒ Some(x)
      },
      path = Option(cookie.getPath) map HttpPathConversion.fromString,
      domain = Option(cookie.getDomain) flatMap trimToOption,
      httpOnly = Some(cookie.isHttpOnly),
      secure = Some(cookie.getSecure)
    )
}
