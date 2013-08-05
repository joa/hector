package hector.util

/**
 * @see http://en.wikipedia.org/wiki/Internet_media_type
 */
object MimeType {
  object application {
    val atomXml = "application/atom+xml"
    val ecmascript = "application/ecmascript"
    val ediX12 = "application/EDI-X12"
    val edifact = "application/EDIFACT"
    val json = "application/json"
    val javascript = "application/javascript"
    val octetStream = "application/octet-stream"
    val ogg = "application/ogg"
    val pdf = "application/pdf"
    val postscript = "application/postscript"
    val rdfXml = "application/rdf+xml"
    val rssXml = "application/rss+xml"
    val soapXml = "application/soap+xml"
    val fontWoff = "application/font-woff"
    val xhtmlXml = "application/xhtml+xml"
    val xmlDTD = "application/xml-dtd"
    val xopXML = "application/xop+xml"
    val zip = "application/zip"
    val xGzip = "application/x-gzip"
    val xWwwFormUrlencoded = "application/x-www-form-urlencoded"
    val xDvi = "application/x-dvi"
    val xLatex = "application/x-latex"
    val xFontTtf = "application/x-font-ttf"
    val xShockwaveFlash = "application/x-shockwave-flash"
    val xStuffit = "application/x-stuffit"
    val xRarCompressed = "application/x-rar-compressed"
    val xTar = "application/x-tar"
    val xDeb = "application/x-deb"
  }

  object audio {
    val basic = "audio/basic"
    val l24 = "audio/L24"
    val mp4 = "audio/mp4"
    val mpeg = "audio/mpeg"
    val ogg = "audio/ogg"
    val vorbis = "audio/vorbis"
    val xMsWMA = "audio/x-ms-wma"
    val xMsWAX = "audio/x-ms-wax"
    val vndRnRealAudio = "audio/vnd.rn-realaudio"
    val vndWave = "audio/vnd.wave"
    val webm = "audio/webm"
  }

  object image {
    val gif = "image/gif"
    val jpeg = "image/jpeg"
    val pjpeg = "image/pjpeg"
    val png = "image/png"
    val svgXml = "image/svg+xml"
    val tiff = "image/tiff"
    val vndMicrosoftIcon = "image/vnd.microsoft.icon"
  }

  object message {
    val http = "message/http"
    val imdnXml = "message/imdn+xml"
    val partial = "message/partial"
    val rfc822 = "message/rfc822"
  }

  object model {
    val example = "model/example"
    val iges = "model/iges"
    val mesh = "model/mesh"
    val vrml = "model/vrml"
    val x3dBinary = "model/x3d+binary"
    val x3dVrml = "model/x3d+vrml"
    val x3dXml = "model/x3d+xml"
  }

  object multipart {
    val mixed = "multipart/mixed"
    val alternative = "multipart/alternative"
    val related = "multipart/related"
    val formData = "multipart/form-data"
    val signed = "multipart/signed"
    val encrypted = "multipart/encrypted"
  }

  object text {
    val cmd = "text/cmd"
    val css = "text/css"
    val csv = "text/csv"
    val html = "text/html"
    val eventStream = "text/event-stream"
    @deprecated("Use application/javascript instead. However text/javascript is allowed in HTML 4 and 5 and comes unlike application/javascript with cross-browser support.", "RFC 4329")
    val javascript = "text/javascript"
    val plain = "text/plain"
    val vcard = "text/vcard"
    val xml = "text/xml"
    val xGwtRpc = "text/x-gwt-rpc"
    val xJQueryTmpl = "text/x-jquery-tmpl"
  }

  object video {
    val mpeg = "video/mpeg"
    val mp4 = "video/mp4"
    val ogg = "video/ogg"
    val quicktime = "video/quicktime"
    val webm = "video/webm"
    val xMatroska = "video/x-matroska"
    val xMsWMV = "video/x-ms-wmv"
  }
}
