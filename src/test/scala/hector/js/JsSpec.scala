package hector.js

import hector.HectorSpec

/**
 */
final class JsSpec extends HectorSpec {
  describe("JsEmitter") {
    it("can emit a simple program") {
      import implicits._

      val ast =
        JsProgram(
          JsIf(
            test = (true: JsExpression) :== (false: JsExpression),
            trueCase = ('window ~> 'alert)("true == false"),
            falseCase = None
          )
          &
          JsIf(
            test = (true: JsExpression) != (false: JsExpression),
            trueCase = ('window ~> 'alert)("true != false"),
            falseCase = None
          )
        )

      ast.emit() must be ("if(true==false)window.alert('true == false');if(true!=false)window.alert('true != false')")
    }

    it("can convert a Map to a JsObj") {
      import implicits._

      val ast: JsAST =
        Map[JsIdentifier, JsExpression]('Abc → "Def", 'X → true)

      ast.emit() must be ("{Abc:'Def',X:true}")
    }
  }
}
