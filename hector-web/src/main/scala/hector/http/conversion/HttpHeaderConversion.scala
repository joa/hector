package hector.http.conversion

import hector.http._

import javax.servlet.http.HttpServletRequest

object HttpHeaderConversion {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): Seq[HttpHeader] = {
    val headerNameEnum = httpServletRequest.getHeaderNames
    
    var result = Vector.empty[HttpHeader]
    
    while(headerNameEnum.hasMoreElements) {
      val headerName = headerNameEnum.nextElement()
      val headerValueEnum = httpServletRequest.getHeaders(headerName)
      
      while(headerValueEnum.hasMoreElements) {
        val headerValue = headerValueEnum.nextElement()
        
        result = result :+ fromNameAndValue(headerName, headerValue)
      }
    }
    
    result
  }
  
  def fromNameAndValue(name: String, value: String) = {
    import hector.http.header._

    name.toLowerCase match {
      case "accept" ⇒ Accept(value)
      case "accept-charset" ⇒ AcceptCharset(value)
      case "accept-encoding" ⇒ AcceptEncoding(value)
      case "accept-language" ⇒ AcceptLanguage(value)
      case "accept-datetime" ⇒ AcceptDatetime(value)
      case "authorization" ⇒ Authorization(value)
      case "cookie" ⇒ Cookie(value)
      case "expect" ⇒ Expect(value)
      case "from" ⇒ From(value)
      case "host" ⇒ Host(value)
      case "if-match" ⇒ IfMatch(value)
      case "if-modified-since" ⇒ IfModifiedSince(value)
      case "if-none-match" ⇒ IfNoneMatch(value)
      case "if-range" ⇒ IfRange(value)
      case "if-unmodified-since" ⇒ IfUnmodifiedSince(value)
      case "max-forwards" ⇒ MaxForwards(value)
      case "proxy-authorization" ⇒ ProxyAuthorization(value)
      case "range" ⇒ Range(value)
      case "referer" ⇒ Referer(value)
      case "te" ⇒ TE(value)
      case "upgrade" ⇒ Upgrade(value)
      case "user-agent" ⇒ UserAgent(value)
      case "x-requested-with" ⇒ XRequestedWith(value)
      case "x-do-not-track" ⇒ XDoNotTrack(value)
      case "dnt" ⇒ DNT(value)
      case "x-forwarded-for" ⇒ XForwardedFor(value)
      case "x-att-deviceid" ⇒ XATTDeviceId(value)
      case "x-wap-profile" ⇒ XWapProfile(value)
      case unknown ⇒ CustomHeader(unknown, value)
    }
  }
}
