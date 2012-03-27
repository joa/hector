package hector.http

/**
 * The HttpHeader class defines a header.
 *
 * <p>Headers can be part of an HttpRequest or an HttpResponse. Standard headers have already been
 * defined and marker traits are used to explain whether or not they are typically used in a
 * request or a response.</p>
 *
 * <p>Several headers like <code>Via(...)</code> can be part of the request and the response.</p>
 */
abstract class HttpHeader(val name: String, val value: String) extends Serializable

/**
 * The CustomHeader class can be used to create a custom HttpHeader.
 *
 * @param name The name of the header.
 * @param value The value of the header.
 */
final case class CustomHeader(override val name: String, override val value: String) extends HttpHeader(name, value)

/**
 * Marker trait for request headers.
 */
trait HttpRequestHeader extends Serializable

/**
 * Marker trait for response headers.
 */
trait HttpResponseHeader extends Serializable
