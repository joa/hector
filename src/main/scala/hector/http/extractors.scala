package hector.http

trait MethodBasedPathExtractor {
  def extract(request: HttpRequest, method: HttpMethod): Option[HttpPath] =
    if(request.method == method) {
      Some(request.path)
    } else {
      None
    }

}

object extractors {
  //TODO(joa): does this belong into a routing package?

  object Options extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Options)
  }

  object Get extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Get)
  }

  object Head extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Head)
  }

  object Post extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Post)
  }

  object Put extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Put)
  }

  object Delete extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Delete)
  }

  object Trace extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Trace)
  }

  object Connect extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = extract(request, HttpMethods.Connect)
  }

  object Any extends MethodBasedPathExtractor {
    def unapply(request: HttpRequest): Option[HttpPath] = Some(request.path)
  }
}
