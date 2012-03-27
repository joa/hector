package hector.http

sealed trait HttpMethod extends Serializable

object HttpMethods {
  case object Options extends HttpMethod
  case object Get extends HttpMethod
  case object Head extends HttpMethod
  case object Post extends HttpMethod
  case object Put extends HttpMethod
  case object Delete extends HttpMethod
  case object Trace extends HttpMethod
  case object Connect extends HttpMethod
}
