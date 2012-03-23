package hector.http

/**
 * The HttpCookie class represents a cookie.
 *
 * @author Joa Ebert
 */
final case class HttpCookie(
  /** The name of the cookie. */
  name: String,
  /** The value of the cookie. */
  value: String,
  /** The maximum age of the cookie in seconds. */
  maxAge: Option[Int],
  /** The path the cookie is valid for. */
  path: Option[HttpPath],
  /** The domain the cookie is valid for. */
  domain: Option[String],
  /** Whether or not the cookie is only valid for HTTP requests and not exposed to client-side scripting. */
  httpOnly: Option[Boolean],
  /** Whether or not the cookie should be transmitted only for secure connections like HTTPS/SSL. */
  secure: Option[Boolean])

object HttpCookieConversion {
  import javax.servlet.http.Cookie

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
}
