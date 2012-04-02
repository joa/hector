package hector.http

trait HttpRequest extends Serializable {
  def method: HttpMethod

  def path: HttpPath

  def cookies: Seq[HttpCookie]

  def headers: Seq[HttpHeader]

  def authType: Option[HttpAuthType]
}
