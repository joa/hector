package hector.config

import akka.util.Timeout
import akka.util.duration._

import hector.http.HttpRequest
import hector.actor.RouterActor.Route
import hector.session.backends.SessionRamBackend
import hector.Hector
import hector.session.SessionBackend
import com.google.common.base.Charsets

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

  /** How long a session is kept alive. */
  def sessionLifetime = 1.minute

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

  def runMode: RunMode =
    System.getProperty("hector.runMode", "").toLowerCase match {
      case "" | "local" | "dev" | "development" ⇒ RunModes.Development
      case "prod" | "production" ⇒ RunModes.Production
      case "stage" | "staging" ⇒ RunModes.Staging
      case "test" | "unittest" | "testing" ⇒ RunModes.Testing
    }
}
