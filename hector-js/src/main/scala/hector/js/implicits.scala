package hector.js

/**
 */
object implicits {
  implicit def jsExpToJsStmt(jsExp: JsExpression): JsStatement = JsExpStatement(jsExp)

  implicit def stringToJsString(string: String): JsString = JsString(string)

  implicit def numericToJsNumeric[T](value: T)(implicit numeric: Numeric[T]): JsNumber[T] = JsNumber(value)

  implicit def booleanToJsBool(boolean: Boolean): JsBool = if(boolean) JsTrue else JsFalse

  implicit def symbolToJsIdentifier(symbol: Symbol): JsIdentifier = JsIdentifier(symbol)

  implicit def seqToArray(seq: Seq[JsExpression]): JsArray = JsArray(seq)

  implicit def statementToBlockStatement(statement: JsStatement): JsBlock = JsBlock(statementToSeq(statement))

  //implicit def expressionToBlockStatement(expression: JsExpression): JsBlock = JsBlock(expressionToSeqOfStatements(expression))

  implicit def statementToSeq(statement: JsStatement): Seq[JsStatement] = Seq(statement)

  implicit def astToSeq[T <% JsAST](ast: T): Seq[T] = Seq(ast)

  implicit def astToOption[T <% JsAST](ast: T): Option[T] = Some(ast)

  implicit def expressionToSeqOfStatements(expression: JsExpression): Seq[JsStatement] = Seq(jsExpToJsStmt(expression))

  implicit def mapToJsObj(map: Map[JsIdentifier, JsExpression]): JsObj =
    JsObj((map.view map { tuple ⇒ JsProp(tuple._1, tuple._2) }).toSeq)

  implicit def symbolAndSymbolToJsIdentifierAndJsSymbol(t2: (Symbol, Symbol)): (JsIdentifier,  JsIdentifier) = (JsIdentifier(t2._1), JsIdentifier(t2._2))

  implicit def symbolAndBooleanToJsIdentifierAndJsBool(t2: (Symbol, Boolean)): (JsIdentifier,  JsBool) = (JsIdentifier(t2._1), booleanToJsBool(t2._2))

  implicit def symbolAndNumericToJsIdentifierAndJsNumber[T](t2: (Symbol, T))(implicit numeric: Numeric[T]): (JsIdentifier,  JsNumber[T]) = (JsIdentifier(t2._1), JsNumber(t2._2))

  implicit def symbolAndStringToJsIdentifierAndJsString(t2: (Symbol, String)): (JsIdentifier,  JsString) = (JsIdentifier(t2._1), JsString(t2._2))
}
