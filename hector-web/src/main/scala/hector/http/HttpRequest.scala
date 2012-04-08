package hector.http

trait HttpRequest extends Serializable {
  def method: HttpMethod

  def path: HttpPath

  def cookies: Seq[HttpCookie]

  def headers: Seq[HttpHeader]

  def authType: Option[HttpAuthType]

  def info: HttpRequestInfo = new HttpRequestInfo(this)
}

@inline
final class HttpRequestInfo(request: HttpRequest) {
  /** Whether or not the client would not like to be tracked. */
  def doNotTrack: Boolean =
    request.headers exists { 
      case XDoNotTrack(_) | DNT(_) => true
      case _ => false
    }
  
  /** Whether or not the request was made using Android. */
  def isAndroid: Boolean = false

  /** Whether or not the request was made using iOS. */
  def isIOS: Boolean = false

  /** Whether or not the request was made using Windows. */
  def isWindows: Boolean = false

  /** Whether or not the request was made using Linux. */
  def isLinux: Boolean = false

  /** Whether or not the request was made using OS X.*/
  def isOSX: Boolean = false
}
