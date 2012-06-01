package hector

import akka.actor._

import hector.actor._
import hector.config._
import js.JsIdentifier

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
    case exception ⇒
      system.log.error(exception, "Could not initialize configuration.")
      throw exception
  }

  val root: ActorRef = system.actorOf(Props[RootActor], "hector") // For now ...

  def start() {
    config.preStart()

    system.log.info("********************************")
    system.log.info("*           HECTOR             *")
    system.log.info("********************************")

    if(config.runMode < RunModes.Staging) {
      system.eventStream.subscribe(system.actorOf(Props(new Actor {
        def receive = {
          case deadLetter: DeadLetter ⇒
            //TODO(joa): what about a helper mode?
            system.log.warning("Received dead letter: {}", deadLetter)
        }
      })), classOf[DeadLetter])
    }

    root ! "run"

    config.postStart()
  }

  def stop() {
    config.preStop()

    system.log.info("Shutdown initiated ...")

    stopRoot()
    system.shutdown()

    config.postStop()
  }

  private[this] def stopRoot(trial: Int = 0) {
    import akka.actor.ActorTimeoutException
    import akka.dispatch.{Await, Future}
    import akka.pattern.gracefulStop
    import akka.util.duration._

    try {
      val stopped: Future[Boolean] = gracefulStop(root, 5.seconds)(system)
      Await.result(stopped, 6.seconds)
    } catch {
      case timeout: ActorTimeoutException ⇒
        (trial + 1) match {
          case maxTrials if maxTrials > 2 ⇒ 
            system.log.warning("Could not stop root actor.")
            
          case retry ⇒ 
            system.log.warning("Could not shutdown root actor. Retrying ...")
            stopRoot(retry)
        }
    }
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
  def session = system.actorFor("/user/hector/sessionStorage")

  /** Actor responsible for session signals. */
  def sessionSignals = system.actorFor("/user/hector/sessionSignals")

  /** Actor responsible for JavaScript callbacks. */
  def callback = system.actorFor("/user/hector/callback")

  /** Actor responsible for gathering statistics. */
  def statistics = system.actorFor("/user/hector/stats")

  /** Actor responsible for HTML 5 event streams. */
  def eventStream = system.actorFor("/hector/eventStream")

  /**  Prefix for internal actions. */
  val internalPrefix = Hector.config.hectorInternal

  /** Script that allows client-server communication. */
  val clientSupport = {
    import hector.js._
    import hector.js.toplevel.{jsWindow ⇒ window, jsDocument ⇒ document, jsEval ⇒ eval}
    import hector.js.toplevel._
    import hector.js.implicits._

    @inline def vars(nameAndValues: (JsIdentifier, JsExpression)*) =
      JsVars(nameAndValues map { nameAndValue ⇒ JsVar(nameAndValue._1, Some(nameAndValue._2)) })

    @inline def anonymousFunction(parameters: JsIdentifier*)(body: JsStatement*) =
      JsFunc(None, parameters, JsBlock(body))

    @inline def log(level: String, message: JsExpression): JsStatement =
      if(Hector.config.runMode < RunModes.Production) {
        (('console : JsIdentifier) ~> 'log)(JsString("[HECTOR-"+level+"]: ")+message)
      } else {
        JsEmptyStatement
      }

    @inline def block(statements: JsStatement*): JsBlock = JsBlock(statements)

    /*
    We represent the following script using the JsAST and minifying compiler built-in to hector.

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
    */

    val hectorVariable = JsIdentifier('hector)

    val doNotContinueIfHectorVariableIsAlreadyDefined = {
      //
      // This will simply exit a function if the Hector variable has already
      // been defined.
      //
      // It is used as a guard so that multiple instances of clientSupport on the same
      // page do not clash.
      //

      JsIf(
        window ~> hectorVariable,
        trueCase = JsReturn()
      )
    }

    val execCallback: JsExpression = {
      //
      // List of identifiers
      //

      val xhr = JsIdentifier('xhr)
      val contentType = JsIdentifier('contentType)
      val root = JsIdentifier('root)
      val body = JsIdentifier('body)
      val n = JsIdentifier('n)
      val i = JsIdentifier('i)

      //
      // Some conditions we want to give a name
      //

      val status = JsThis ~> 'status
      val statusCodeIsSuccess = 200 <= status && status < 300
      val contentTypeIsJavaScript = (contentType :== "application/javascript") || (contentType :== "text/javascript")
      val contentTypeIsXml = contentType :== "text/xml"

      // The execCallback function takes a parameter "name" which is the name of the
      // callback. Callbacks are executed by performing a POST request on /{hectorInternal}/cb/{name}
      // which will trigger the server logic.
      //
      // For that case we simply
      //
      anonymousFunction('name)(
        vars(xhr → JsNew('XMLHttpRequest)),
        (xhr ~> 'open)("POST", JsString("/"+internalPrefix+"/cb/") + 'name, true),
        xhr('onload) = anonymousFunction('e)(
          JsIf(
            statusCodeIsSuccess,
            trueCase = block(
              log("DEBUG", JsThis ~> 'response),
              vars(contentType → (JsThis ~> 'getResponseHeader)("Content-Type")),
              JsIf(
                contentTypeIsJavaScript,
                trueCase = eval(JsThis ~> 'response),
                falseCase = JsIf(contentTypeIsXml, trueCase = block(
                  vars(root → (JsThis ~> 'responseXML), body → document.body),
                  JsFor(vars(i → 0, n → (root ~> 'childNodes ~> 'length)), Some(i < n), Some(i.++),
                    (body ~> 'appendChild)((document ~> 'importNode)(JsArrayAccess(root ~> 'childNodes, 'i), true))
                  )
                ))
              )
            )
          )
        ),
        (xhr ~> 'send)()
      )
    }

    val bindHectorToTheWindowObject =
      window(hectorVariable) = Map(
        JsIdentifier('execCallback) → execCallback
      )

    val ast =
      anonymousFunction(/* no parameters */)(
        doNotContinueIfHectorVariableIsAlreadyDefined,
        bindHectorToTheWindowObject
      )()

    ast.toNode(humanReadable = false)
  }
}
