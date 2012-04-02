package hector.http

/**
 */
sealed trait HttpAuthType extends Serializable

object HttpAuthTypes {
  case object Basic extends HttpAuthType
  case object Form extends HttpAuthType
  case object ClientCertificate extends HttpAuthType
  case object Digest extends HttpAuthType
}
