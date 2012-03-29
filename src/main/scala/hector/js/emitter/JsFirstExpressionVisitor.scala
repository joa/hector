package hector.js.emitter

import hector.js._

/**
 */
object JsFirstExpressionVisitor {
  def exec(statement: JsExpStatement): Boolean =
    statement match {
      case JsExpStatement(JsFunc(_, _, _)) => false
      case JsExpStatement(exp) => visit(exp)
    }

  private[this] def visit(ast: JsAST): Boolean =
    ast match {
      case JsArrayAccess(array, _) => visit(array)
      case JsBinary(left, _, _) => visit(left)
      case JsCondition(test, _, _) => visit(test)
      case JsCall(callee, _) => visit(callee)
      case JsMember(obj, _) => visit(obj)
      case JsPostfix(_, exp) => visit(exp)
      case JsFunc(_, _, _) | JsObj(_) => true
      case _ => false
    }
}
