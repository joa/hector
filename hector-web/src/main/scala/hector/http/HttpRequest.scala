package hector.http

import com.google.common.collect.ImmutableSortedSet

trait HttpRequest extends Serializable {
  def method: HttpMethod

  def path: HttpPath

  def cookies: Seq[HttpCookie]

  def headers: Seq[HttpHeader]

  def authType: Option[HttpAuthType]

  def info: HttpRequestInfo = new HttpRequestInfo(this)

  //TODO(joa): def node = The node in the cluster representing the frontend which the client is connected to. 
}

protected[http] object KnownAgentValues {
  val Android = "Android"
  val iPhone = "iPhone"
  val iPad = "iPad"
  val iPod = "iPod"
  val Windows_NT_ = "Windows NT "
  val Windows = Array(
    Windows_NT_ +"6.2", // Windows 8
    Windows_NT_ +"6.1", // Windows 7
    Windows_NT_ +"6.0", // Windows Vista
    Windows_NT_ +"5.2", // Windows Server 2003
    Windows_NT_ +"5.1", "Windows XP", // Windows XP
    Windows_NT_ +"5.0", "Windows 2000", // Windows 2000
    Windows_NT_ +"4.0", "WinNT4.0", "WinNT", "Windows NT", // Windows NT 4.0
    "Win 9x 4.90", "Windows ME", // Windows ME
    "Windows 98", "Win98",  // Windows 98
    "Windows 95", "Win95", "Windows_95", // Windows 95
    "Win16" // Windows 3.11
  )
  val Mac_OS_X_ = "Mac OS X "
  val OSX = Array(
      Mac_OS_X_ +"10.7", // Lion
      Mac_OS_X_ +"10.6", // Snow Leopard
      Mac_OS_X_ +"10.5", // Leopard
      Mac_OS_X_ +"10.4", // Tiger
      Mac_OS_X_ +"10.3", // Panther
      Mac_OS_X_ +"10.2", // Jaguar
      Mac_OS_X_ +"10.1", // Puma
      Mac_OS_X_ +"10.0", // Ceetah
      Mac_OS_X_ +"Beta"  // Kodiak
  )
  val MobileAgents = ImmutableSortedSet.of("TODO http://www.zytrax.com/tech/web/mobile_ids.html")
}

@inline
final class HttpRequestInfo(request: HttpRequest) {
  import header._
  import KnownAgentValues._

  /** Whether or not the client would not like to be tracked. */
  def doNotTrack: Boolean =
    request.headers exists { 
      case XDoNotTrack(_) | DNT(_) => true
      case _ => false
    }

  private[this] def agentContains(value: String): Boolean =
    request.headers exists { 
      case UserAgent(agentString) if agentString contains value => true
      case _ => false
    }

  private[this] def agentContains(value: Array[String]): Boolean =
    request.headers exists {
      case UserAgent(agentString) if arrayContains(agentString, value) => true
      case _ => false
    }

  private[this] def arrayContains(agentString: String, array: Array[String]): Boolean = {
    var i = 0
    val n = array.length

    while(i < n) {
      if(agentString contains array(i)) {
        return true
      }

      i += 1
    }

    false
  }

  /** Whether or not the request was made using Android. */
  def isAndroid: Boolean = agentContains(Android)

  /** Whether or not the request was made using iOS. */
  def isIOS: Boolean = isIPhone || isIPad

  /** Whether or not the request was made using an iPhone. */
  def isIPhone: Boolean = agentContains(iPhone)

  /** Whether or not the request was made using an iPad. */
  def isIPad: Boolean = agentContains(iPad)

  /** Whether or not the request was made using an iPod. */
  def isIPod: Boolean = agentContains(iPod)

  /** Whether or not the request was made using Windows. */
  def isWindows: Boolean = agentContains(Windows_NT_) || agentContains(Windows)

  /** Whether or not the request was made using Linux. */
  def isLinux: Boolean = isUbuntu || agentContains("Linux") || agentContains("X11")

  /** Whether or not the request was made using Ubuntu. */
  def isUbuntu: Boolean = agentContains("Ubuntu")

  /** Whether or not the request was made using OS X.*/
  def isOSX: Boolean = agentContains(Mac_OS_X_) || agentContains(OSX) || 
    agentContains("Macintosh") || agentContains("Mac_PowerPC")

  /** Whether or not the request was made using a mobile device like a phone or tablet. */
  def isMobile: Boolean =
    request.headers exists {
      case UserAgent(agentString) if MobileAgents contains agentString => true
      case _ => false
    }

  /*
  http://www.geekpedia.com/code47_Detect-operating-system-from-user-agent-string.html
  http://www.dannyherran.com/2011/02/detect-mobile-browseruser-agent-with-php-ipad-iphone-blackberry-and-others/

  function detect_mobile()
{
    if(preg_match('/(alcatel|amoi|android|avantgo|blackberry|benq|cell|cricket|docomo|elaine|htc|iemobile|iphone|ipad|ipaq|ipod|j2me|java|midp|mini|mmp|mobi|motorola|nec-|nokia|palm|panasonic|philips|phone|sagem|sharp|sie-|smartphone|sony|symbian|t-mobile|telus|up\.browser|up\.link|vodafone|wap|webos|wireless|xda|xoom|zte)/i', $_SERVER['HTTP_USER_AGENT']))
        return true;
 
    else
        return false;
}
*/
}
