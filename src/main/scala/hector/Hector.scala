package hector

import hector.session.SessionActor

import akka.routing.{DefaultResizer, RoundRobinRouter}
import akka.actor.{Props, ActorSystem}
import hector.actor._

/**
 * @author Joa Ebert
 */
object Hector {
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
  val system = ActorSystem("hector")

  val requests =
    system.actorOf(
      Props[RequestActor].
        withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 32, upperBound = 64)))), name = "requests")

  /**
   * Actor responsible for handling session storage.
   */
  val session =
    system.actorOf(
      Props[SessionActor].
        withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))), name = "session")

  val callback =
    system.actorOf(
      Props[CallbackActor].
        withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))), name = "callback")

  val statistics =
    system.actorOf(
      Props[StatisticsActor],
      name = "statistics"
    )

  val eventStream =
    system.actorOf(
      Props[EventStreamSupervisor],
      name = "eventStream"
    )

  val utilities =
    system.actorOf(
      Props[UtilityActor].
        withRouter(
          RoundRobinRouter(resizer = Some(DefaultResizer(lowerBound = 1, upperBound = 10)))), name = "utilities")

  /**
   * Prefix for internal actions.
   */
  val internalPrefix = "__hector__"

  /**
   * Script that allows client-server communication.
   */
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
