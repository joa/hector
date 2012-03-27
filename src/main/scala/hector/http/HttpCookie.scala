package hector.http

/**
 * The HttpCookie class represents a cookie.
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
