package hector.js.emitter

import hector.js._

import javax.annotation.concurrent.ThreadSafe

/**
 */
@ThreadSafe
private[emitter] object JsPrecedenceVisitor {
  val PrecedenceOfNew = 15

  def apply(expression: JsExpression): Int =
    expression match {
      case JsArrayAccess(_, _) => 16
      case JsArray(_) => 17
      case JsBinary(_, op, _) => op.precedence
      case JsTrue | JsFalse => 17
      case JsCondition(_, _, _) => 3
      case JsFunc(_, _, _) => 17
      case JsCall(_, _) => 16
      case JsMember(_, _) => 16
      case JsIdentifier(_) => 17
      case JsNew(_, _) => PrecedenceOfNew
      case JsNull => 17
      case JsNumber(_) => 17
      case JsObj(_) => 17
      case JsPostfix(op, _) => op.precedence
      case JsPrefix(op, _) => op.precedence
      //case JsRegEx(_) => 17
      case JsString(_) => 17
      case JsThis => 17
      case JsNop(exp) => apply(exp)
      case noPrecedence => sys.error("Error: "+noPrecedence+" has no defined precedence.")
    }
}
