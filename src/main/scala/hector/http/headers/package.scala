package hector.http.headers

import hector.http.{HttpHeader, HttpRequestHeader, HttpResponseHeader}

// Standard request headers:
final case class Accept(override val value: String) extends HttpHeader("Accept", value) with HttpRequestHeader
final case class AcceptCharset(override val value: String) extends HttpHeader("Accept-Charset", value) with HttpRequestHeader
final case class AcceptEncoding(override val value: String) extends HttpHeader("Accept-Encoding", value) with HttpRequestHeader
final case class AcceptLanguage(override val value: String) extends HttpHeader("Accept-Language", value) with HttpRequestHeader
final case class AcceptDatetime(override val value: String) extends HttpHeader("Accept-Datetime", value) with HttpRequestHeader
final case class Authorization(override val value: String) extends HttpHeader("Authorization", value) with HttpRequestHeader
final case class Cookie(override val value: String) extends HttpHeader("Cookie", value) with HttpRequestHeader
final case class Expect(override val value: String) extends HttpHeader("Expect", value) with HttpRequestHeader
final case class From(override val value: String) extends HttpHeader("From", value) with HttpRequestHeader
final case class Host(override val value: String) extends HttpHeader("Host", value) with HttpRequestHeader
final case class IfMatch(override val value: String) extends HttpHeader("If-Match", value) with HttpRequestHeader
final case class IfModifiedSince(override val value: String) extends HttpHeader("If-Modified-Since", value) with HttpRequestHeader
final case class IfNoneMatch(override val value: String) extends HttpHeader("If-None-Match", value) with HttpRequestHeader
final case class IfRange(override val value: String) extends HttpHeader("If-Range", value) with HttpRequestHeader
final case class IfUnmodifiedSince(override val value: String) extends HttpHeader("If-Unmodified-Since", value) with HttpRequestHeader
final case class MaxForwards(override val value: String) extends HttpHeader("Max-Forwards", value) with HttpRequestHeader
final case class ProxyAuthorization(override val value: String) extends HttpHeader("Proxy-Authorization", value) with HttpRequestHeader
final case class Range(override val value: String) extends HttpHeader("Range", value) with HttpRequestHeader
final case class Referer(override val value: String) extends HttpHeader("Referer", value) with HttpRequestHeader
final case class TE(override val value: String) extends HttpHeader("TE", value) with HttpRequestHeader
final case class Upgrade(override val value: String) extends HttpHeader("Upgrade", value) with HttpRequestHeader
final case class UserAgent(override val value: String) extends HttpHeader("User-Agent", value) with HttpRequestHeader

// De-facto standard request headers:
final case class XRequestedWith(override val value: String) extends HttpHeader("X-Requested-With", value) with HttpRequestHeader
final case class XDoNotTrack(override val value: String) extends HttpHeader("X-Do-Not-Track", value) with HttpRequestHeader
final case class DNT(override val value: String) extends HttpHeader("DNT", value) with HttpRequestHeader
final case class XForwardedFor(override val value: String) extends HttpHeader("X-Forwarded-For", value) with HttpRequestHeader
final case class XATTDeviceId(override val value: String) extends HttpHeader("X-ATT-DeviceId", value) with HttpRequestHeader
final case class XWapProfile(override val value: String) extends HttpHeader("X-Wap-Profile", value) with HttpRequestHeader

// Standard response headers:
final case class AcceptRanges(override val value: String) extends HttpHeader("Accept-Ranges", value) with HttpResponseHeader
final case class Age(override val value: String) extends HttpHeader("Age", value) with HttpResponseHeader
final case class Allow(override val value: String) extends HttpHeader("Allow", value) with HttpResponseHeader
final case class ContentEncoding(override val value: String) extends HttpHeader("Content-Encoding", value) with HttpResponseHeader
final case class ContentLanguage(override val value: String) extends HttpHeader("Content-Language", value) with HttpResponseHeader
final case class ContentLocation(override val value: String) extends HttpHeader("Content-Location", value) with HttpResponseHeader
final case class ContentDisposition(override val value: String) extends HttpHeader("Content-Disposition", value) with HttpResponseHeader
final case class ContentRange(override val value: String) extends HttpHeader("Content-Range", value) with HttpResponseHeader
final case class ETag(override val value: String) extends HttpHeader("ETag", value) with HttpResponseHeader
final case class Expires(override val value: String) extends HttpHeader("Expires", value) with HttpResponseHeader
final case class LastModified(override val value: String) extends HttpHeader("Last-Modified", value) with HttpResponseHeader
final case class Link(override val value: String) extends HttpHeader("Link", value) with HttpResponseHeader
final case class Location(override val value: String) extends HttpHeader("Location", value) with HttpResponseHeader
final case class P3P(override val value: String) extends HttpHeader("P3P", value) with HttpResponseHeader
final case class ProxyAuthenticate(override val value: String) extends HttpHeader("Proxy-Authenticate", value) with HttpResponseHeader
final case class Refresh(override val value: String) extends HttpHeader("Refresh", value) with HttpResponseHeader
final case class RetryAfter(override val value: String) extends HttpHeader("Retry-After", value) with HttpResponseHeader
final case class Server(override val value: String) extends HttpHeader("Server", value) with HttpResponseHeader
final case class SetCookie(override val value: String) extends HttpHeader("Set-Cookie", value) with HttpResponseHeader
final case class StrictTransportSecurity(override val value: String) extends HttpHeader("Strict-Transport-Security", value) with HttpResponseHeader
final case class Trailer(override val value: String) extends HttpHeader("Trailer", value) with HttpResponseHeader
final case class TransferEncoding(override val value: String) extends HttpHeader("Transfer-Encoding", value) with HttpResponseHeader
final case class Vary(override val value: String) extends HttpHeader("Vary", value) with HttpResponseHeader
final case class WWWAuthenticate(override val value: String) extends HttpHeader("WWW-Authenticate", value) with HttpResponseHeader

// De-facto standard response headers:
final case class XFrameOptions(override val value: String) extends HttpHeader("X-Frame-Options", value) with HttpResponseHeader
final case class XXSSProtection(override val value: String) extends HttpHeader("X-XSS-Protection", value) with HttpResponseHeader
final case class XContentTypeOptions(override val value: String) extends HttpHeader("X-Content-Type-Options", value) with HttpResponseHeader
final case class XForwardedProto(override val value: String) extends HttpHeader("X-Forwarded-Proto", value) with HttpResponseHeader
final case class XPoweredBy(override val value: String) extends HttpHeader("X-Powered-By", value) with HttpResponseHeader
final case class XUACompatible(override val value: String) extends HttpHeader("X-UA-Compatible", value) with HttpResponseHeader

// Standard Response/Request headers:
final case class CacheControl(override val value: String) extends HttpHeader("Cache-Control", value) with HttpRequestHeader with HttpResponseHeader
final case class Connection(override val value: String) extends HttpHeader("Connection", value) with HttpRequestHeader with HttpResponseHeader
final case class ContentLength(override val value: String) extends HttpHeader("Content-Length", value) with HttpRequestHeader with HttpResponseHeader
final case class ContentMD5(override val value: String) extends HttpHeader("Content-MD5", value) with HttpRequestHeader with HttpResponseHeader
final case class ContentType(override val value: String) extends HttpHeader("Content-Type", value) with HttpRequestHeader with HttpResponseHeader
final case class Date(override val value: String) extends HttpHeader("Date", value) with HttpRequestHeader with HttpResponseHeader
final case class Pragma(override val value: String) extends HttpHeader("Pragma", value) with HttpRequestHeader with HttpResponseHeader
final case class Via(override val value: String) extends HttpHeader("Via", value) with HttpRequestHeader with HttpResponseHeader
final case class Warning(override val value: String) extends HttpHeader("Warning", value) with HttpRequestHeader with HttpResponseHeader
