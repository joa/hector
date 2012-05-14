package hector

import akka.actor.{ActorSystem, Props}

import hector.actor.route.Route
import hector.config.HectorConfig
import hector.http.extractors._
import hector.http._
import hector.pattern._

import user.HelloWorldActor

/**
 */
final class Configuration extends HectorConfig {
  private[this] val helloWorld = Hector.system.actorOf(Props[HelloWorldActor])//.withRouter(RoundRobinRouter(nrOfInstances = 128)))

  def routes = {
    case Get("user" /: publicKey /: _) ⇒ Route(helloWorld, Some(publicKey))
    case Get(Required_/ | No_/) ⇒ respond(
      HtmlResponse(
        <html>
          <head>
            <title>Hello World!</title>
          </head>
          <body>
            Visit the <a href="/user/test" target="_self" title="Test's profile">test</a> user profile.
          </body>
        </html>
      )
    )
    //case _ ⇒ <html><title>404!</title><body>404</body></html>
  }
}
