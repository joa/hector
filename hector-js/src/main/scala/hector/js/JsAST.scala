package hector.js

import scala.language.dynamics

import hector.js.emitter.JsEmitter

import scala.xml.Unparsed
import scala.Char


/**
 */
sealed trait JsAST extends Serializable {
  import scala.xml.Node

  def emit(humanReadable: Boolean = false): String = JsEmitter.toString(this, humanReadable)

  def toNode(humanReadable: Boolean = false): Node = <script type="text/javascript">{Unparsed(emit(humanReadable))}</script>

  def toStatement: JsStatement
}

case class JsProgram(statements: Seq[JsStatement]) extends JsAST {
  def toStatement = JsBlock(statements)
}

sealed trait JsPropertyKind
case object JsGetProperty extends JsPropertyKind
case object JsSetProperty extends JsPropertyKind

sealed trait JsStatement extends JsAST {
  def and(that: Seq[JsStatement]): Seq[JsStatement] = &(that)
  def and(that: JsStatement): Seq[JsStatement] = &(that)
  def &(that: Seq[JsStatement]): Seq[JsStatement] = this +: that
  def &(that: JsStatement): Seq[JsStatement] = Seq(this, that)
  def toStatement: JsStatement = this
}

case object JsEmptyStatement extends JsStatement
case class JsBlock(stmt: Seq[JsStatement] = Seq.empty) extends JsStatement {
  def toSeq: Seq[JsStatement] = stmt
}

//TODO(joa): get rid of JsExpStatement and the notion of a "JsStatement" since JsExp might as well just extend JsStmt
case class JsExpStatement(exp: JsExpression) extends JsStatement
case class JsIf(test: JsExpression, trueCase: JsStatement, falseCase: Option[JsStatement] = None) extends JsStatement
case class JsLabeledStatement(label: JsIdentifier, body: JsStatement) extends JsStatement
case class JsBreak(label: Option[JsIdentifier] = None) extends JsStatement
case class JsContinue(label: Option[JsIdentifier] = None) extends JsStatement
//TODO(joa): case class JsSwitch(test: JsExpression, cases: Seq[JsSwitchCase])
case class JsReturn(value: Option[JsExpression] = None) extends JsStatement
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
case class JsVar(name: JsIdentifier, init: Option[JsExpression] = None) extends JsStatement

/**
 * var x = y
 * @param vars
 */
case class JsVars(vars: Seq[JsVar]) extends JsStatement

sealed trait JsOperator {
  def isKeyword: Boolean = false
  def symbol: Array[Char]
  def precedence: Int
}

sealed trait JsUnop extends JsOperator

sealed trait JsBinop extends JsOperator {
  def isLeftAssociative: Boolean
}

object JsUnops {
  case object `-` extends JsUnop { override val symbol = Array('-'); override val precedence = 14 }
  case object `+` extends JsUnop { override val symbol = Array('+'); override val precedence = 14 }
  case object `!` extends JsUnop { override val symbol = Array('!'); override val precedence = 14 }
  case object `~` extends JsUnop { override val symbol = Array('~'); override val precedence = 14 }
  case object `typeof` extends JsUnop { override def isKeyword = true; override val symbol = "typeof".toCharArray; override val precedence = 14 }
  case object `void` extends JsUnop { override def isKeyword = true; override val symbol = "void".toCharArray; override val precedence = 14 }
  case object `delete` extends JsUnop { override def isKeyword = true; override val symbol = "delete".toCharArray; override val precedence = 14 }
  case object `++` extends JsUnop { override val symbol = Array('+', '+'); override val precedence = 14 }
  case object `--` extends JsUnop { override val symbol = Array('-', '-'); override val precedence = 14 }
}

object JsBinops {
  case object `==` extends JsBinop { override val symbol = Array('=', '='); override val isLeftAssociative = true; override val precedence = 9 }
  case object `!=` extends JsBinop { override val symbol = Array('!', '='); override val isLeftAssociative = true; override val precedence = 9 }
  case object `===` extends JsBinop { override val symbol = Array('=', '=', '='); override val isLeftAssociative = true; override val precedence = 9 }
  case object `!==` extends JsBinop { override val symbol = Array('!', '=', '='); override val isLeftAssociative = true; override val precedence = 9 }
  case object `<` extends JsBinop { override val symbol = Array('<'); override val isLeftAssociative = true; override val precedence = 10 }
  case object `<=` extends JsBinop { override val symbol = Array('<', '='); override val isLeftAssociative = true; override val precedence = 10 }
  case object `>` extends JsBinop { override val symbol = Array('>'); override val isLeftAssociative = true; override val precedence = 10 }
  case object `>=` extends JsBinop { override val symbol = Array('>', '='); override val isLeftAssociative = true; override val precedence = 10 }
  case object `in` extends JsBinop { override def isKeyword = true; override val symbol = Array('i', 'n'); override val isLeftAssociative = true; override val precedence = 10 }
  case object `instanceof` extends JsBinop { override def isKeyword = true; override val symbol = "instanceof".toCharArray; override val isLeftAssociative = true; override val precedence = 10 }
  case object `<<` extends JsBinop { override val symbol = Array('<', '<'); override val isLeftAssociative = true; override val precedence = 11 }
  case object `>>` extends JsBinop { override val symbol = Array('>', '>'); override val isLeftAssociative = true; override val precedence = 11 }
  case object `>>>` extends JsBinop { override val symbol = Array('>', '>', '>'); override val isLeftAssociative = true; override val precedence = 11 }
  case object `+` extends JsBinop { override val symbol = Array('+'); override val isLeftAssociative = true; override val precedence = 12 }
  case object `-` extends JsBinop { override val symbol = Array('-'); override val isLeftAssociative = true; override val precedence = 12 }
  case object `*` extends JsBinop { override val symbol = Array('*'); override val isLeftAssociative = true; override val precedence = 13 }
  case object `/` extends JsBinop { override val symbol = Array('/'); override val isLeftAssociative = true; override val precedence = 13 }
  case object `%` extends JsBinop { override val symbol = Array('%'); override val isLeftAssociative = true; override val precedence = 13 }
  case object `|` extends JsBinop { override val symbol = Array('|'); override val isLeftAssociative = true; override val precedence = 6 }
  case object `^` extends JsBinop { override val symbol = Array('^'); override val isLeftAssociative = true; override val precedence = 7 }
  case object `&` extends JsBinop { override val symbol = Array('&'); override val isLeftAssociative = true; override val precedence = 8 }
  case object `,` extends JsBinop { override val symbol = Array(','); override val isLeftAssociative = true; override val precedence = 1 }
  case object `||` extends JsBinop { override val symbol = Array('|', '|'); override val isLeftAssociative = true; override val precedence = 4 }
  case object `&&` extends JsBinop { override val symbol = Array('&', '&'); override val isLeftAssociative = true; override val precedence = 5 }
  case object `=` extends JsBinop { override val symbol = Array('='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `+=` extends JsBinop { override val symbol = Array('+', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `-=` extends JsBinop { override val symbol = Array('-', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `*=` extends JsBinop { override val symbol = Array('*', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `/=` extends JsBinop { override val symbol = Array('/', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `%=` extends JsBinop { override val symbol = Array('%', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `<<=` extends JsBinop { override val symbol = Array('<', '<', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `>>=` extends JsBinop { override val symbol = Array('>', '>', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `>>>=` extends JsBinop { override val symbol = Array('>', '>', '>', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `|=` extends JsBinop { override val symbol = Array('|', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `^=` extends JsBinop { override val symbol = Array('^', '='); override val isLeftAssociative = false; override val precedence = 2 }
  case object `&=` extends JsBinop { override val symbol = Array('&', '='); override val isLeftAssociative = false; override val precedence = 2 }
}

trait JsAssignments {
  self: JsExpression ⇒

  def set(value: JsExpression) = :=(value)

  def :=(value: JsExpression) = JsBinary(this, JsBinops.`=`, value)
  def :+=(value: JsExpression) = JsBinary(this, JsBinops.`+=`, value)
  def :-=(value: JsExpression) = JsBinary(this, JsBinops.`-=`, value)
  def :*=(value: JsExpression) = JsBinary(this, JsBinops.`*=`, value)
  def :/=(value: JsExpression) = JsBinary(this, JsBinops.`/=`, value)
  def :%=(value: JsExpression) = JsBinary(this, JsBinops.`%=`, value)
  def :<<=(value: JsExpression) = JsBinary(this, JsBinops.`<<=`, value)
  def :>>=(value: JsExpression) = JsBinary(this, JsBinops.`>>=`, value)
  def :>>>=(value: JsExpression) = JsBinary(this, JsBinops.`>>>=`, value)
  def :|=(value: JsExpression) = JsBinary(this, JsBinops.`|=`, value)
  def :^=(value: JsExpression) = JsBinary(this, JsBinops.`^=`, value)
  def :&=(value: JsExpression) = JsBinary(this, JsBinops.`&=`, value)
}

sealed trait JsExpression extends JsAST with Dynamic {
  def toStatement: JsStatement = JsExpStatement(this)

  def unary_- = JsPrefix(JsUnops.`-`, this)
  def unary_+ = JsPrefix(JsUnops.`+`, this)
  def unary_! = JsPrefix(JsUnops.`!`, this)
  def unary_~ = JsPrefix(JsUnops.`~`, this)
  def unary_typeof = JsPrefix(JsUnops.`typeof`, this)
  def unary_void = JsPrefix(JsUnops.`void`, this)
  def unary_delete = JsPrefix(JsUnops.`delete`, this)
  def unary_++ = JsPrefix(JsUnops.`++`, this)
  def unary_-- = JsPrefix(JsUnops.`--`, this)
  def ++ = JsPostfix(JsUnops.`++`, this)
  def -- = JsPostfix(JsUnops.`--`, this)

  //Hitting a compiler bug in 2.9.1 with "==" so "==" is named ":=="
  def :==(that: JsExpression) = JsBinary(this, JsBinops.`==`, that)
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


  // note: since scala cannot differentiate between a(i) and a[i] we have no choice and make the
  // tradeoff for a JsCall instead of JsArrayAccess

  def apply(arguments: JsExpression*) = JsCall(this, arguments)

  def update(name: JsIdentifier, value: JsExpression) = JsBinary(JsMember(this, name), JsBinops.`=`, value)

  def applyDynamic(name: String)(args: Any*) =
    JsCall(JsMember(this, JsIdentifier(Symbol(name))), args map { _.asInstanceOf[JsExpression]})

  def selectDynamic(name: String) =
    JsMember(this, JsIdentifier(Symbol(name)))

  def updateDynamic(name: String)(value: Any) =
    JsBinary(JsMember(this, JsIdentifier(Symbol(name))), JsBinops.`=`, value.asInstanceOf[JsExpression])

  def ~>(property: JsIdentifier): JsMember = JsMember(this, property)
}
case class JsIdentifier(name: Symbol) extends JsExpression with JsAssignments
case class JsNop(expression: JsExpression) extends JsExpression // useful to upcast an expression with a trait!
case object JsThis extends JsExpression
case class JsArrayAccess(array: JsExpression, index: JsExpression) extends JsExpression
case class JsProp(key: JsIdentifier, value: JsExpression, kind: Option[JsPropertyKind] = None)
case class JsObj(properties: Seq[JsProp]) extends JsExpression
case class JsFunc(id: Option[JsIdentifier], parameters: Seq[JsIdentifier], body: JsBlock) extends JsExpression
case class JsBinary(left: JsExpression, op: JsBinop, right: JsExpression) extends JsExpression
case class JsPrefix(op: JsUnop, exp: JsExpression) extends JsExpression
case class JsPostfix(op: JsUnop, exp: JsExpression) extends JsExpression
case class JsCondition(test: JsExpression, trueCase: JsExpression, falseCase: JsExpression) extends JsExpression
case class JsNew(constructor: JsExpression, arguments: Seq[JsExpression] = Seq.empty) extends JsExpression
case class JsCall(callee: JsExpression, arguments: Seq[JsExpression] = Seq.empty) extends JsExpression
case class JsMember(obj: JsExpression, property: JsIdentifier) extends JsExpression with JsAssignments
sealed trait JsLiteral extends JsExpression
case class JsString(value: String) extends JsLiteral
case class JsArray(elements: Seq[JsExpression] = Seq.empty) extends JsLiteral
sealed trait JsBool extends JsLiteral
case object JsTrue extends JsBool
case object JsFalse extends JsBool
case object JsNull extends JsLiteral
case class JsNumber[T](value: T)(implicit numeric: Numeric[T]) extends JsLiteral {
  def numberOps = numeric
}
//TODO(joa): case class JsRegEx(value: String) extends JsLiteral

// No E4X
