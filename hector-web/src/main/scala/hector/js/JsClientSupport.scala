package hector.js

import hector.config.RunModes

/**
 * The JsClientSupport$ object is a JsProgram which is used to support communication between
 * client and server.
 *
 * <p>The following code is represented by the JsProgram:</p>
 *
 * <p><code><pre>
 * (function(){
 *   if(window.hector) return
 *
 *   var h = {
 *     execCallback: function(name) {
 *       var xhr = new XMLHttpRequest()
 *       xhr.open('POST', '/"""+internalPrefix+"""/cb/'+name, true)
 *       xhr.onload = function(e) {
 *         if(200 <= this.status && this.status < 300) {
 *           console.log('[DEBUG]: '+this.response)
 *           switch(this.getResponseHeader('Content-Type')) {
 *             case 'application/javascript':
 *             case 'text/javascript':
 *               eval(this.response)
 *               break
 *             case 'text/xml':
 *               var root = this.responseXML
 *               var body = document.body
 *               for(var i = 0, n = root.childNodes.length; i < n; i++) {
 *                 body.appendChild(document.importNode(root.childNodes[i], true))
 *               }
 *               break
 *           }
 *         }
 *       }
 *       xhr.send()
 *     }
 *   }
 *
 *   window.hector = h
 * })();
 * <pre/></code></p>
 */
 object ScalacBug {
  def beWaterMyFriend: Seq[JsStatement] = {
    import hector.Hector
    import hector.js.toplevel.{jsWindow ⇒ window, jsDocument ⇒ document, jsEval ⇒ eval}
    import hector.js.implicits._

    //
    // vars(name0 -> value0, name1 -> value1) will result in "var name0 = value0, name1 = value1"
    //
    @inline def vars(nameAndValues: (JsIdentifier, JsExpression)*) =
      JsVars(nameAndValues map { nameAndValue ⇒ JsVar(nameAndValue._1, Some(nameAndValue._2)) })

    //
    // Creates and returns an anonymous function.
    //

    @inline def anonymousFunction(parameters: JsIdentifier*)(body: JsStatement*) =
      JsFunc(None, parameters, JsBlock(body))

    @inline def log(level: String, message: JsExpression): JsStatement =
      if(Hector.config.runMode < RunModes.Production) {
        (('console : JsIdentifier) ~> 'log)(JsString(s"[HECTOR-${level}]: ")+message)
      } else {
        JsEmptyStatement
      }

    //
    // Creates and returns a JsBlock for a sequence of functions.
    //

    @inline def block(statements: JsStatement*): JsBlock = JsBlock(statements)


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

      //
      // The execCallback function takes a parameter "name" which is the name of the
      // callback. Callbacks are executed by performing a POST request on /{hectorInternal}/cb/{name}
      // which will trigger the server logic.
      //
      // If text/javascript ot application/javascript is received the code will be evaluated. If it
      // is text/xml instead the nodes are appended to the current document.
      //

      anonymousFunction('name)(
        vars(xhr → JsNew('XMLHttpRequest)),
        (xhr ~> 'open)("POST", JsString(s"/${Hector.config.hectorInternal}/cb/") + 'name, true),
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

    ast
  }
}

object JsClientSupport extends JsProgram(ScalacBug.beWaterMyFriend)
