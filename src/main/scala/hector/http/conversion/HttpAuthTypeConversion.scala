package hector.http.conversion

import hector.http._
import hector.http.HttpAuthTypes._

import javax.servlet.http.HttpServletRequest

/**
 */
object HttpAuthTypeConversion {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): Option[HttpAuthType] =
    fromString(httpServletRequest.getAuthType)

  def fromString(authType: String): Option[HttpAuthType] =
    Option(authType) map { _.toLowerCase } flatMap {
      case "basic" ⇒ Some(Basic)
      case "form" ⇒ Some(Form)
      case "client_cert" ⇒ Some(ClientCertificate)
      case "digest" ⇒ Some(Digest)
      case _ ⇒ None
    }
}
