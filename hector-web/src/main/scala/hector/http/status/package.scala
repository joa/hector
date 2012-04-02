package hector.http

package object status {
  // Informational
  val Continue = 100
  val SwitchingProtocols = 101
  val Processing = 102
  val Checkpoint = 103

  // Success
  val Ok = 200
  val Created = 201
  val Accepted = 202
  val NonAuthoritativeInformation = 203
  val NoContent = 204
  val ResetContent = 205
  val PartialContent = 206
  val MultiStatus = 207
  val AlreadyReported = 208
  val IMUsed = 226

  // Redirection
  val MultipleChoices = 300
  val MovedPermanently = 301
  val Found = 302
  val SeeOther = 303
  val NotModified = 304
  val UseProxy = 305
  val SwitchProxy = 306
  val TemporaryRedirect = 307
  val ResumeIncomplete = 308

  // Client Error
  val BadRequest = 400
  val Unauthorized = 401
  val PaymentRequired = 402
  val Forbidden = 403
  val NotFound = 404
  val MethodNotAllowed = 405
  val NotAcceptable = 406
  val ProxyAuthenticationRequired = 4007
  val RequestTimeout = 408
  val Conflict = 409
  val Gone = 410
  val LengthRequired = 411
  val PreconditionFailed = 412
  val RequestEntityTooLarge = 413
  val RequestURITooLong = 414
  val UnsupportedMediaType = 415
  val RequestedRangeNotSatisfiable = 416
  val ExpectationFailed = 417
  val UnprocessableEntity = 422
  val Locked = 423
  val FailedDependency = 424
  val UnorderedCollection = 425
  val UpgradeRequired = 426
  val PreconditionRequired = 428
  val TooManyRequests = 429
  val RequestHeaderFieldsTooLarge = 431
  val NoResponse = 444
  val RetryWith = 449
  val ClientClosedRequest = 499

  // Server Error
  val InternalServerError = 500
  val NotImplemented = 501
  val BadGateway = 502
  val ServiceUnavailable = 503
  val GatewayTimeout = 504
  val HttpVersionNotSupported = 505
  val VariantAlsoNegotiates = 506
  val InsufficientStorage = 507
  val LoopDetected = 508
  val BandwidthLimitExceeded = 509
  val NotExtended = 510
  val NetworkAuthenticationRequired = 511
  val NetworkReadTimeout = 598
  val NetworkConnectionTimeout = 599
}
