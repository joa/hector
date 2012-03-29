package hector.js

import scala.xml.Node

/**
 */
sealed trait JsAST extends Serializable {
  def emit(humanReadable: Boolean = false): String = JsEmitter.toString(this, humanReadable)
}

case class JsProgram(statements: Seq[JsStatement]) extends JsAST

sealed trait JsPropertyKind
case object JsGetProperty extends JsPropertyKind
case object JsSetProperty extends JsPropertyKind

sealed trait JsStatement extends JsAST {
  def and(that: Seq[JsStatement]): Seq[JsStatement] = &(that)
  def and(that: JsStatement): Seq[JsStatement] = &(that)
  def &(that: Seq[JsStatement]): Seq[JsStatement] = this +: that
  def &(that: JsStatement): Seq[JsStatement] = Seq(this, that)
}

case object JsEmptyStatement extends JsStatement
case class JsBlock(stmt: Seq[JsStatement]) extends JsStatement
case class JsExpStatement(exp: JsExpression) extends JsStatement
case class JsIf(test: JsExpression, trueCase: JsStatement, falseCase: Option[JsStatement]) extends JsStatement
case class JsLabeledStatement(label: JsIdentifier, body: JsStatement) extends JsStatement
case class JsBreak(label: Option[JsIdentifier]) extends JsStatement
case class JsContinue(label: Option[JsIdentifier]) extends JsStatement
case class JsWith(obj: JsExpression, body: JsStatement) extends JsStatement
//TODO(joa): case class JsSwitch(test: JsExpression, cases: Seq[JsSwitchCase])
case class JsReturn(value: Option[JsExpression]) extends JsStatement
case class JsThrow(value: JsExpression) extends JsStatement
case class JsWhile(test: JsExpression, body: JsStatement) extends JsStatement
case class JsDoWhile(test: JsExpression,  body: JsStatement) extends JsStatement
case class JsFor(init: Option[JsStatement], test: Option[JsStatement], update: Option[JsStatement], body: JsStatement) extends JsStatement
case class JsForIn(left: JsStatement, right: JsExpression,  body: JsStatement) extends JsStatement
//TODO(joa): case class JsCatchStmt(body: JsStatement) extends JsStatement
//TODO(joa): case class JsTry(body: JsBlock, handlers: Seq[JsCatchStmt], fin: Option[JsStatement]) extends JsStatement
/**
 * x = y
 * @param name
 * @param init
 */
case class JsVar(name: JsIdentifier, init: Option[JsExpression]) extends JsStatement

/**
 * var x = y
 * @param vars
 */
case class JsVars(vars: Seq[JsVar]) extends JsStatement

sealed trait JsOperator {
  def isKeyword: Boolean = false
  def symbol: Array[Char]
}

sealed trait JsPreOrPostfixOperator extends JsOperator

object JsPreOrPostfixOperators {
  case object `++` extends JsPreOrPostfixOperator { override val symbol = Array('+', '+') }
  case object `--` extends JsPreOrPostfixOperator { override val symbol = Array('-', '-') }
}

sealed trait JsUnop extends JsOperator

object JsUnops {
  case object `-` extends JsUnop { override val symbol = Array('-') }
  case object `+` extends JsUnop { override val symbol = Array('+') }
  case object `!` extends JsUnop { override val symbol = Array('!') }
  case object `~` extends JsUnop { override val symbol = Array('~') }
  case object `typeof` extends JsUnop { override def isKeyword = true; override val symbol = "typeof".toCharArray }
  case object `void` extends JsUnop { override def isKeyword = true; override val symbol = "void".toCharArray }
  case object `delete` extends JsUnop { override def isKeyword = true; override val symbol = "delete".toCharArray }
}

sealed trait JsBinop extends JsOperator {
  def isLeftAssociative: Boolean = false //TODO(joa): implement in objects

  def precedence: Int = 0 //TODO(joa): implement in objects
}

object JsBinops {
  case object `==` extends JsBinop { override val symbol = Array('=', '=') }
  case object `!=` extends JsBinop { override val symbol = Array('!', '=') }
  case object `===` extends JsBinop { override val symbol = Array('=', '=', '=') }
  case object `!==` extends JsBinop { override val symbol = Array('!', '=', '=') }
  case object `<` extends JsBinop { override val symbol = Array('<') }
  case object `<=` extends JsBinop { override val symbol = Array('<', '=') }
  case object `>` extends JsBinop { override val symbol = Array('>') }
  case object `>=` extends JsBinop { override val symbol = Array('>', '=') }
  case object `<<` extends JsBinop { override val symbol = Array('<', '<') }
  case object `>>` extends JsBinop { override val symbol = Array('>', '>') }
  case object `>>>` extends JsBinop { override val symbol = Array('>', '>', '>') }
  case object `+` extends JsBinop { override val symbol = Array('+') }
  case object `-` extends JsBinop { override val symbol = Array('-') }
  case object `*` extends JsBinop { override val symbol = Array('*') }
  case object `/` extends JsBinop { override val symbol = Array('/') }
  case object `%` extends JsBinop { override val symbol = Array('%') }
  case object `|` extends JsBinop { override val symbol = Array('|') }
  case object `^` extends JsBinop { override val symbol = Array('^') }
  case object `,` extends JsBinop { override val symbol = Array(',') }
  case object `in` extends JsBinop { override def isKeyword = true; override val symbol = Array('i', 'n') }
  case object `instanceof` extends JsBinop { override def isKeyword = true; override val symbol = Array('=', '=') }
  case object `||` extends JsBinop { override val symbol = Array('|', '|') }
  case object `&&` extends JsBinop { override val symbol = Array('&', '&') }
}

sealed trait JsAssignmentOp extends JsOperator

object JsAssignmentOps {
  case object `=` extends JsAssignmentOp { override val symbol = Array('=') }
  case object `+=` extends JsAssignmentOp { override val symbol = Array('+', '=') }
  case object `-=` extends JsAssignmentOp { override val symbol = Array('-', '=') }
  case object `*=` extends JsAssignmentOp { override val symbol = Array('*', '=') }
  case object `/=` extends JsAssignmentOp { override val symbol = Array('/', '=') }
  case object `%=` extends JsAssignmentOp { override val symbol = Array('%', '=') }
  case object `<<=` extends JsAssignmentOp { override val symbol = Array('<', '<', '=') }
  case object `>>=` extends JsAssignmentOp { override val symbol = Array('>', '>', '=') }
  case object `>>>=` extends JsAssignmentOp { override val symbol = Array('>', '>', '>', '=') }
  case object `|=` extends JsAssignmentOp { override val symbol = Array('|', '=') }
  case object `^=` extends JsAssignmentOp { override val symbol = Array('^', '=') }
  case object `&=` extends JsAssignmentOp { override val symbol = Array('&', '=') }
}

trait JsAssignments {
  self: JsExpression ⇒

  def set(value: JsExpression) = :=(value)

  def :=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`=`, value)
  def :+=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`+=`, value)
  def :-=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`-=`, value)
  def :*=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`*=`, value)
  def :/=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`/=`, value)
  def :%=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`%=`, value)
  def :<<=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`<<=`, value)
  def :>>=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`>>=`, value)
  def :>>>=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`>>>=`, value)
  def :|=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`|=`, value)
  def :^=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`^=`, value)
  def :&=(value: JsExpression) = JsAssignment(this, JsAssignmentOps.`&=`, value)
}

sealed trait JsExpression extends JsAST/* with Dynamic*/ {
  def unary_- = JsUnary(JsUnops.`-`, this)
  def unary_+ = JsUnary(JsUnops.`+`, this)
  def unary_! = JsUnary(JsUnops.`!`, this)
  def unary_~ = JsUnary(JsUnops.`~`, this)
  def unary_typeof = JsUnary(JsUnops.`typeof`, this)
  def unary_void = JsUnary(JsUnops.`void`, this)
  def unary_delete = JsUnary(JsUnops.`delete`, this)

  def ==(that: JsExpression) = JsBinary(this, JsBinops.`==`, that)
  def !=(that: JsExpression) = JsBinary(this, JsBinops.`!=`, that)
  def ===(that: JsExpression) = JsBinary(this, JsBinops.`===`, that)
  def !==(that: JsExpression) = JsBinary(this, JsBinops.`!==`, that)
  def <(that: JsExpression) = JsBinary(this, JsBinops.`<`, that)
  def <=(that: JsExpression) = JsBinary(this, JsBinops.`<=`, that)
  def >(that: JsExpression) = JsBinary(this, JsBinops.`>`, that)
  def >=(that: JsExpression) = JsBinary(this, JsBinops.`>=`, that)
  def <<(that: JsExpression) = JsBinary(this, JsBinops.`<<`, that)
  def >>(that: JsExpression) = JsBinary(this, JsBinops.`>>`, that)
  def >>>(that: JsExpression) = JsBinary(this, JsBinops.`>>>`, that)
  def +(that: JsExpression) = JsBinary(this, JsBinops.`+`, that)
  def -(that: JsExpression) = JsBinary(this, JsBinops.`-`, that)
  def *(that: JsExpression) = JsBinary(this, JsBinops.`*`, that)
  def /(that: JsExpression) = JsBinary(this, JsBinops.`/`, that)
  def %(that: JsExpression) = JsBinary(this, JsBinops.`%`, that)
  def |(that: JsExpression) = JsBinary(this, JsBinops.`|`, that)
  def ^(that: JsExpression) = JsBinary(this, JsBinops.`^`, that)
  def in(that: JsExpression) = JsBinary(this, JsBinops.`in`, that)
  def instanceof(that: JsExpression) = JsBinary(this, JsBinops.`instanceof`, that)
  def ||(that: JsExpression) = JsBinary(this, JsBinops.`||`, that)
  def &&(that: JsExpression) = JsBinary(this, JsBinops.`&&`, that)

  def unary_++ = JsPrefix(JsPreOrPostfixOperators.`++`, this)
  def unary_-- = JsPrefix(JsPreOrPostfixOperators.`--`, this)
  def ++ = JsPostfix(JsPreOrPostfixOperators.`++`, this)
  def -- = JsPostfix(JsPreOrPostfixOperators.`--`, this)

  def apply(arguments: JsExpression*) = JsCall(this, arguments)

  def update(name: JsIdentifier, value: JsExpression) = JsAssignment(JsMember(this, name), JsAssignmentOps.`=`, value)

  /*
  def applyDynamic(name: String)(args: Any*) = {
    //XXX(joa): unless we have no way to distinguish between field and method access it is better to get rid of this!
    require(args.isEmpty, "Unfortunately you have to use the syntax (obj.member)(arg) instead of obj.member(arg).")
    JsMember(this, JsIdentifier(Symbol(name)))
  }
  */

  def ~>(property: JsIdentifier): JsMember = JsMember(this, property)
}
case class JsIdentifier(name: Symbol) extends JsExpression with JsAssignments
case class JsNop(expression: JsExpression) extends JsExpression // useful to upcast an expression with a trait!
case object JsThis extends JsExpression
case class JsArrayAccess(array: JsExpression, index: JsExpression) extends JsExpression
case class JsProp(key: JsIdentifier, value: JsExpression, kind: Option[JsPropertyKind] = None)
case class JsObj(properties: Seq[JsProp]) extends JsExpression
case class JsFunc(id: Option[JsIdentifier], parameters: Seq[JsIdentifier], body: JsBlock) extends JsExpression
case class JsSeq(exp: Seq[JsExpression]) extends JsExpression
case class JsUnary(op: JsUnop, value: JsExpression) extends JsExpression
case class JsBinary(left: JsExpression, op: JsBinop, right: JsExpression) extends JsExpression
case class JsAssignment(left: JsExpression, op: JsAssignmentOp, right: JsExpression) extends JsExpression
case class JsPrefix(op: JsPreOrPostfixOperator, exp: JsExpression) extends JsExpression
case class JsPostfix(op: JsPreOrPostfixOperator, exp: JsExpression) extends JsExpression
case class JsCondition(test: JsExpression, trueCase: JsExpression, falseCase: JsExpression) extends JsExpression
case class JsNew(constructor: JsExpression, arguments: Seq[JsExpression]) extends JsExpression
case class JsCall(callee: JsExpression, arguments: Seq[JsExpression]) extends JsExpression
case class JsMember(obj: JsExpression, property: JsIdentifier) extends JsExpression with JsAssignments
sealed trait JsLiteral extends JsExpression
case class JsString(value: String) extends JsLiteral
case class JsArray(elements: Seq[JsExpression]) extends JsLiteral {
  //TODO(joa): where to put this? can we add it at all?
  // def apply(index: JsExpression) = JsArrayAccess(this, index)
  // def update(index: JsExpression, value: JsExpression) = JsAssignment(JsArrayAccess(this, index), JsAssignmentOps.`=`, value)
}
sealed trait JsBool extends JsLiteral
case object JsTrue extends JsBool
case object JsFalse extends JsBool
case object JsNull extends JsLiteral
case class JsNumber[T](value: T)(implicit numeric: Numeric[T]) extends JsLiteral {
  def numberOps = numeric
}
//TODO(joa): case class JsRegEx(value: String) extends JsLiteral

// No E4X
