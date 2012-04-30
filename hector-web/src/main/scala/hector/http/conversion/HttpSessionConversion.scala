package hector.http.conversion

import hector.Hector
import hector.http._
import hector.session._
import hector.util.randomHash

import javax.servlet.http.HttpServletRequest

object HttpSessionConversion {
  def fromHttpServletRequest(httpServletRequest: HttpServletRequest): Option[HttpSession] =
    Hector.config.sessionBackend map {
      backend ⇒
        val existingId =
          for {
            cookies ← Option(httpServletRequest.getCookies())
            sessionCookie ← cookies find { _.getName == Hector.config.sessionCookieName }
          } yield {
            sessionCookie.getValue
          }

        val id = existingId getOrElse randomHash()

        Hector.session ! SessionActor.KeepAlive(id)

        new HttpSession(id)
    }
}
