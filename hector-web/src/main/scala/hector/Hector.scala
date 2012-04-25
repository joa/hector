package hector

import hector.session.SessionActor

import hector.actor._
import hector.config.HectorConfig
import akka.pattern.ask
import scala.Serializable
import akka.actor._
import akka.dispatch.Future
import hector.http.HttpRequest

/**
 */
object Hector {
  val system = ActorSystem("hector")

  val config: HectorConfig = try {
    val className = System.getProperty("hector.config", "hector.Configuration")

    try {
      val klass = Class.forName(className)

      if(!classOf[HectorConfig].isAssignableFrom(klass)) {
        throw new RuntimeException("Error: "+className+" has to extend "+classOf[HectorConfig].getName)
      }

      klass.newInstance().asInstanceOf[HectorConfig]
    } catch {
      case classNotFound: ClassNotFoundException ⇒ throw new RuntimeException("Error: Class "+className+" could not be found.")
      case linkageError: LinkageError ⇒ throw new RuntimeException("Error: Could not link class "+className+".")
      case instantiationException: InstantiationException ⇒ throw new RuntimeException("Error: Could not instantiate "+className+". Make sure it is a class and has a zero-arguments constructor.")
    }
  } catch {
    case exception =>
      system.log.error(exception, "Could not initialize configuration.")
      throw exception
  }

  val root: ActorRef = system.actorOf(Props[RootActor], "hector") // For now ...

  def main() {
    system.log.info("********************************")
    system.log.info("*           HECTOR             *")
    system.log.info("********************************")

    system.eventStream.subscribe(system.actorOf(Props(new Actor {
      def receive = {
        case deadLetter: DeadLetter ⇒
          //TODO(joa): what about a helper mode?
          system.log.warning("Received dead letter: {}", deadLetter)
      }
    })), classOf[DeadLetter])

    root ! "run"
  }

  //TODO(joa): get rid of me after on
  /*import com.typesafe.config.ConfigFactory
  val config = ConfigFactory.parseString("""
    akka.loglevel = DEBUG
    akka.actor.debug {
      receive = on
      lifecycle = on
    }
    """)

  val system = ActorSystem("hector", config)*/

  /** Actor responsible for handling Http requests. */
  def request = system.actorFor("/user/hector/request")

  /** Actor responsible for session storage. */
  def session = system.actorFor("/user/hector/session")

  /** Actor responsible for JavaScript callbacks. */
  def callback = system.actorFor("/user/hector/callback")

  /** Actor responsible for gathering statistics. */
  def statistics = system.actorFor("/user/hector/stats")

  /** Actor responsible for HTML 5 event streams. */
  def eventStream = system.actorFor("/hector/eventStream")

  //TODO(joa): those two methods should reside somewhere else
  def sessionStore[V <: Serializable](request: HttpRequest, key: String, value: V): Future[Unit] = {
    ask(session, SessionActor.Store(request, key, value))(Hector.config.defaultSessionTimeout).mapTo[Unit]
  }

  def sessionLoad[V <: Serializable](request: HttpRequest, key: String)(implicit manifest: Manifest[V]): Future[V] = {
    ask(session, SessionActor.Load(request, key))(Hector.config.defaultSessionTimeout).mapTo[V]
  }

  /**  Prefix for internal actions. */
  val internalPrefix = "__hector__"

  /** Script that allows client-server communication. */
  val clientSupport = {
    import scala.xml.Unparsed

    <script type="text/javascript">{Unparsed(
"""
(function(){
  if(window.hector) return

  var h = {
    execCallback: function(name) {
      var xhr = new XMLHttpRequest()
      xhr.open('POST', '/"""+internalPrefix+"""/cb/'+name, true)
      xhr.onload = function(e) {
        if(200 <= this.status && this.status < 300) {
          console.log('[DEBUG]: '+this.response)
          switch(this.getResponseHeader('Content-Type')) {
            case 'application/javascript':
            case 'text/javascript':
              eval(this.response)
              break
            case 'text/xml':
              var root = this.responseXML
              var body = document.body
              for(var i = 0, n = root.childNodes.length; i < n; i++) {
                body.appendChild(document.importNode(root.childNodes[i], true))
              }
              break
          }
        }
      }
      xhr.send()
    }
  }

  window.hector = h
})();
""")}</script>
  }
}
