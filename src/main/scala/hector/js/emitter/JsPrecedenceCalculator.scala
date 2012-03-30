package hector.js.emitter

import hector.js._

import javax.annotation.concurrent.ThreadSafe

/**
 * The JsPrecedenceCalculator determines the precedence for a given JsExpression.
 *
 * <p>Precedence values are the ones specified in the Mozilla online reference.</p>
 *
 * <p>Please note that we use the inverse so Mozilla states that the comma has precedence 17 which
 * means in our case it is precedence 1.</p>
 *
 * @see <a href="https://developer.mozilla.org/en/JavaScript/Reference/Operators/Operator_Precedence">Operator Precedence</a>
 */
@ThreadSafe
private[emitter] object JsPrecedenceCalculator {
  val PrecedenceOfNew = 15

  def apply(expression: JsExpression): Int =
    expression match {
      //TODO(joa): still using broken GWT precedence values

      case JsArray(_) ⇒ 17
      case JsObj(_) ⇒ 17
      case JsTrue | JsFalse ⇒ 17
      case JsFunc(_, _, _) ⇒ 17
      case JsNull ⇒ 17
      case JsNumber(_) ⇒ 17
      case JsIdentifier(_) ⇒ 17
      //case JsRegEx(_) ⇒ 17
      case JsString(_) ⇒ 17
      case JsThis ⇒ 17

      case JsNew(_, _) ⇒ PrecedenceOfNew

      case JsArrayAccess(_, _) ⇒ 16
      case JsMember(_, _) ⇒ 16
      case JsCall(_, _) ⇒ 16

      case JsBinary(_, op, _) ⇒ op.precedence
      case JsCondition(_, _, _) ⇒ 3

      case JsPostfix(op, _) ⇒ op.precedence
      case JsPrefix(op, _) ⇒ op.precedence

      case JsNop(exp) ⇒ apply(exp)

      case noPrecedence ⇒ sys.error("Error: "+noPrecedence+" has no defined precedence.")
    }
}
