package hector

import hector.config.HectorConfig
import hector.http.extractors.Get
import hector.http./:
import hector.actor.RouterActor.Route
import user.HelloWorldActor
import akka.actor.{ActorSystem, Props}

/**
 */
final class Configuration extends HectorConfig {
  private[this] val helloWorld = Hector.system.actorOf(Props[HelloWorldActor])//.withRouter(RoundRobinRouter(nrOfInstances = 128)))

  def routes = {
    case Get("user" /: publicKey /: _) ⇒  Route(helloWorld, Some(publicKey))
    //case _ ⇒ <html><title>404!</title><body>404</body></html>
  }
}
