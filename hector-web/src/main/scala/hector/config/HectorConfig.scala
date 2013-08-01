package hector.config

import akka.util.Timeout

import com.google.common.base.Charsets

import hector.Hector
import hector.actor.route.Route
import hector.http.HttpRequest
import hector.session.backends.SessionRamBackend
import hector.session.SessionBackend

import scala.concurrent._
import scala.concurrent.duration._

/**
 */
abstract class HectorConfig {
  /** The default timeout for a request. */
  def defaultRequestTimeout = Timeout(3.seconds)

  /** The amount of time a response is kept open.
   *
   * Note that a request might be processed quickly in case of an HTML5 EventSource but the
   * response is kept only for the timeout specified here.
   */
  def responseTimeout = Timeout(1.minute)

  /** The default timeout for session storage to reply.
   *
   * <p>Not to be confused with the actual timeout for a session.
   * This timeout is used when asking the storage for data.</p>
   */
  def defaultSessionTimeout = Timeout(1.second)

  /** The name of the session cookie. */
  def sessionCookieName = "__hector_session__"

  /** Whether or not the cookie is only valid for HTTP requests and not exposed to client-side scripting. */
  def sessionCookieHttpOnly = true

  /** Whether or not the cookie should be transmitted only for secure connections like HTTPS/SSL. */
  def sessionCookieSecure = false

  /** How long a session is kept alive. */
  def sessionLifetime = 15.minutes

  /** Timeout for the router to reply. */
  def defaultRouteTimeout = Timeout(1.second)

  /** The user-defined routes. */
  def routes: PartialFunction[HttpRequest, Route[Any]]

  /** The average size of a Html response in chars. */
  def averageHtmlSize: Int = 20000

  /** The session backend. <code>None</code> if your application is completely stateless. */
  def sessionBackend: Option[SessionBackend] = Some(new SessionRamBackend(Hector.system.dispatcher))

  /** The default character set. */
  def defaultCharset = Charsets.UTF_8

  /** The prefix for internal requests. */
  def hectorInternal = "__hector__"

  /** The mode in which Hector runs. */
  def runMode: RunMode =
    System.getProperty("hector.runMode", "").toLowerCase match {
      case "" | "local" | "dev" | "development" ⇒ RunModes.Development
      case "prod" | "production" ⇒ RunModes.Production
      case "stage" | "staging" ⇒ RunModes.Staging
      case "test" | "unittest" | "testing" ⇒ RunModes.Testing
    }

  /** Method executed before Hector starts. */
  def preStart() {}

  /** Method executed after the startup sequence completed. */
  def postStart() {}

  /** Method executed before Hector stops. */
  def preStop() {}

  /** Method executed after the shutdown sequence completed. */
  def postStop() {}
}
