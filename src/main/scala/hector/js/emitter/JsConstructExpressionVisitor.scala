package hector.js.emitter

import hector.js._


/**
 */
private[emitter] object JsConstructExpressionVisitor {
  def apply(expression: JsExpression): Boolean =
    if(JsPrecedenceVisitor(expression) < JsPrecedenceVisitor.PrecedenceOfNew) {
      true
    } else {
      visit(expression)
    }

  private[this] def visit(ast: JsAST): Boolean =
    ast match {
      case JsArrayAccess(array, _) => visit(array)
      case JsCall(_, _) => true
      case JsMember(obj, _) => visit(obj)
      case JsNop(exp) => visit(exp)
      case _ => false
    }
}
