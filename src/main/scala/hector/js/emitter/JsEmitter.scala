package hector.js.emitter

import hector.js._
import hector.util.escapeJavaScriptString

import java.io.{PrintWriter, StringWriter}
import javax.annotation.concurrent.NotThreadSafe

object JsEmitter {
  private[JsEmitter] val CharsBreak = "break".toCharArray

  private[JsEmitter] val CharsCase = "case".toCharArray

  private[JsEmitter] val CharsCatch = "catch".toCharArray

  private[JsEmitter] val CharsContinue = "continue".toCharArray

  private[JsEmitter] val CharsDebugger = "debugger".toCharArray

  private[JsEmitter] val CharsDefault = "default".toCharArray

  private[JsEmitter] val CharsDo = "do".toCharArray

  private[JsEmitter] val CharsElse = "else".toCharArray

  private[JsEmitter] val CharsFalse = "false".toCharArray

  private[JsEmitter] val CharsFinally = "finally".toCharArray

  private[JsEmitter] val CharsFor = "for".toCharArray

  private[JsEmitter] val CharsFunction = "function".toCharArray

  private[JsEmitter] val CharsIf = "if".toCharArray

  private[JsEmitter] val CharsIn = "in".toCharArray

  private[JsEmitter] val CharsNew = "new".toCharArray

  private[JsEmitter] val CharsNull = "null".toCharArray

  private[JsEmitter] val CharsReturn = "return".toCharArray

  private[JsEmitter] val CharsSwitch = "switch".toCharArray

  private[JsEmitter] val CharsThis = "this".toCharArray

  private[JsEmitter] val CharsThrow = "throw".toCharArray

  private[JsEmitter] val CharsTrue = "true".toCharArray

  private[JsEmitter] val CharsTry = "try".toCharArray

  private[JsEmitter] val CharsVar = "var".toCharArray

  private[JsEmitter] val CharsWhile = "while".toCharArray

  def toNode(ast: JsAST, humanReadable: Boolean = false) = {
    import scala.xml.Unparsed

    <script type="text/javascript">
      {Unparsed(data = toString(ast, humanReadable))}
    </script>
  }

  def toString(ast: JsAST, humanReadable: Boolean = false) = {
    val stringWriter = new StringWriter()
    val printWriter = new PrintWriter(stringWriter)
    val writer = new JsWriter(printWriter, humanReadable)

    val emitter = new JsEmitter()
    emitter.visit(ast)(writer)

    printWriter.flush()
    printWriter.close()
    stringWriter.toString
  }
}

/**
 */
@NotThreadSafe
private final class JsEmitter {
  import JsEmitter._

  // this is the only variable keeping us from making this an object and threadsafe ...!
  private[this] var needSemi = false

  // The following code is a port of JsToStringGenerationVisitor from the GWT source code.
  //

  //TODO create function for following pattern:
  //_nestedPush(body, false)
  //visit(body)
  //_nestedPop(body)

  //TODO(joa): get rid of code duplicates ...
  def visit(ast: JsAST)(implicit writer: JsWriter) {
    ast match {
      case JsProgram(statements) ⇒
        val last = statements.last

        for { statement <- statements } {
          needSemi = true

          visit(statement)

          _finish(statement, last)
        }

      case JsEmptyStatement ⇒
      // Nothing.

      case JsBlock(statements) ⇒
        _blockOpen()
        val last = statements.last

        for {statement <- statements} {
          needSemi = true

          visit(statement)

          _finish(statement, last)
        }
        _blockClose()

      case x @ JsExpStatement(exp) ⇒
        val surroundWithParentheses = JsFirstExpressionVisitor(x)

        if(surroundWithParentheses) {
          _lparen()
        }

        visit(exp)

        if(surroundWithParentheses) {
          _rparen()
        }

      case JsIf(test, trueCase, falseCase) ⇒
        _if()
        _spaceOpt()
        _lparen()
        visit(test)
        _rparen()
        _nestedPush(trueCase, false)
        visit(trueCase)
        _nestedPop(trueCase)
        falseCase foreach {
          value =>
            if(needSemi) {
              _semi()
              _newLineOpt()
            } else {
              _spaceOpt()
              needSemi = true
            }

            _else()

            value match {
              case _: JsIf =>
                _space()
                visit(value)
              case _ =>
                _nestedPush(value, true)
                visit(value)
                _nestedPop(value)
            }
        }

      case JsLabeledStatement(label, body) ⇒
        _ident(label)
        _colon()
        _spaceOpt()
        visit(body)

      case JsBreak(label) ⇒
        _break()
        label foreach {
          value =>
            _space()
            _ident(value)
        }
      case JsContinue(label) ⇒
        _continue()
        label foreach {
          value =>
            _space()
            _ident(value)
        }

      case JsWith(obj, body) ⇒
        //TODO(joa): need to support stupid with-statement
        sys.error("Who on earth would use with(obj) { ... } anyways?!")

      case JsReturn(value) ⇒
        _return()
        value foreach {
          returnValue =>
            _space()
            visit(returnValue)
        }

      case JsThrow(value) ⇒
        _throw()
        _space()
        visit(value)

      case JsWhile(test, body) ⇒
        _while()
        _spaceOpt()
        _lparen()
        visit(test)
        _rparen()
        _nestedPush(body, false)
        visit(body)
        _nestedPop(body)

      case x @ JsDoWhile(test, body) ⇒
        _do()
        _nestedPush(body, true)
        visit(body)
        _nestedPop(body)

        if(needSemi) {
          _semi()
          _newLineOpt()
        } else {
          _spaceOpt()
          needSemi = true
        }

        _while()
        _spaceOpt()
        _lparen()
        visit(test)
        _rparen()

      case JsFor(init, test, update, body) ⇒
        _for()
        _spaceOpt()
        _lparen()

        init foreach visit

        _semi()

        test foreach {
          value =>
            _spaceOpt()
            visit(value)
        }

        _semi()

        update foreach {
          value =>
            _spaceOpt()
            visit(value)
        }

        _rparen()
        _nestedPush(body, false)
        visit(body)
        _nestedPop(body)

      case JsForIn(left, right, body) ⇒
        _for()
        _spaceOpt()
        _lparen()
        visit(left) //TODO(joa): this could be broken, we just have a JsStatement here
        _space()
        _in()
        _space()
        visit(right)
        _rparen()
        _nestedPush(body, false)
        visit(body)
        _nestedPop(body)

      case JsVar(name, init) ⇒
        _ident(name)
        init foreach {
          value =>
            _spaceOpt()
            _assignment()
            _spaceOpt()
            _parenPushIfCommaExpr(value)
            visit(value)
            _parenPopIfCommaExpr(value)
        }

      case JsVars(vars) ⇒
        _var()
        _space()
        var sep = false
        for { variable <- vars } {
          _sepCommaOptSpace(sep)
          sep = true
          visit(variable)
        }

      case JsNop(expr) ⇒
        visit(expr)

      case x @ JsIdentifier(_) ⇒
        _ident(x)

      case JsThis ⇒
        _this()

      case x @ JsArrayAccess(array, index) ⇒
        _parenPush(x, array, false)
        visit(array)
        _parenPop(x, array, false)
        _lbrack()
        visit(index)
        _rbrack()

      case JsObj(properties) ⇒
        _lbrace()

        var sep = false

        for { property <- properties } {
          _sepCommaOptSpace(sep)
          sep = true

          //TODO this is not quite right yet. We can also have string, integral or decimal properties
          visit(property.key)
          _colon()
          _spaceOpt()
          _parenPushIfCommaExpr(property.value)
          visit(property.value)
          _parenPopIfCommaExpr(property.value)
        }

        _rbrace()

      case JsFunc(id, params, body) ⇒
        _function()

        id foreach {
          value =>
            _space()
            _ident(value)
        }

        _lparen()
        _commaSeparated(params)
        _rparen()
        visit(body)
        needSemi = true

      case x @ JsBinary(left, op, right) ⇒
        _parenPush(x, left, !op.isLeftAssociative)
        visit(left)

        if(op.isKeyword) {
          _parenPopOrSpace(x, left, !op.isLeftAssociative)
        } else {
          _parenPop(x, left, !op.isLeftAssociative)
          _spaceOpt()
        }

        writer.print(op.symbol)

        if(_spaceCalc(op, right)) {
          _parenPushOrSpace(x, right, op.isLeftAssociative)
        } else {
          _spaceOpt()
          _parenPush(x, right, op.isLeftAssociative)
        }

        visit(right)
        _parenPop(x, right, op.isLeftAssociative)

      case x @ JsPrefix(op, value) ⇒
        writer.print(op.symbol)
        if(_spaceCalc(op, value)) {
          _space()
        }
        _parenPush(x, value, false)
        visit(value)
        _parenPop(x, value, false)

      case x @ JsPostfix(op, value) ⇒
        _parenPush(x, value, false)
        visit(value)
        _parenPop(x, value, false)
        writer.print(op.symbol)

      case x @ JsCondition(test, trueCase, falseCase) ⇒
        _parenPush(x, test, true)
        visit(test)
        _parenPop(x, test, true)

        _questionMark()

        _parenPush(x, trueCase, false)
        visit(trueCase)
        _parenPop(x, trueCase, false)

        _colon()

        _parenPush(x, falseCase, false)
        visit(falseCase)
        _parenPop(x, falseCase, false)

      case JsNew(constructor, arguments) ⇒
        _new()
        _space()

        val needsParens = JsConstructExpressionVisitor(constructor)

        if(needsParens) {
          _lparen()
        }

        visit(constructor)

        if(needsParens) {
          _rparen()
        }

        if(arguments.nonEmpty) {
          _lparen()
          _commaSeparated(arguments)
          _rparen()
        }

      case x @ JsCall(callee, arguments) ⇒
        _parenPush(x, callee, false)
        visit(callee)
        _parenPop(x, callee, false)
        _lparen()
        _commaSeparated(arguments)
        _rparen()

      case x @ JsMember(obj, property) ⇒
        _parenPush(x, obj, false)
        visit(obj)
        obj match {
          case JsNumber(_) => _space()
          case _ =>
        }
        _parenPop(x, obj, false)
        _dot()
        visit(property)

      case JsString(value) ⇒
        _stringLiteral(value)

      case JsArray(elements) ⇒
        _lbrack()
        _commaSeparated(elements)
        _rbrack()

      case JsTrue ⇒
        _true()

      case JsFalse ⇒
        _false()

      case JsNull ⇒
        _null()

      case x @ JsNumber(value) ⇒
        val doubleValue: Double = x.numberOps.toDouble(value)

        if(doubleValue == 0.0 && (1.0 / doubleValue) == Double.NegativeInfinity) {
          writer.print("-0.")
        } else {
          val longValue: Long = doubleValue.asInstanceOf[Long]
          if(doubleValue == longValue) {
            writer.print(longValue.toString)
          } else {
            writer.print(doubleValue.toString)
          }
        }

      //TODO(joa): case JsRegEx(value) ⇒ print(value)
    }
  }

  private[this] def _commaSeparated(elements: Seq[JsExpression])(implicit writer: JsWriter) {
    var sep = false

    for { expression <- elements } {
      _sepCommaOptSpace(sep)
      sep = true
      _parenPushIfCommaExpr(expression)
      visit(expression)
      _parenPopIfCommaExpr(expression)
    }
  }

  private[this] def _finish(currentStatement: JsStatement, lastStatement: JsStatement)(implicit writer: JsWriter) {
    val isFunctionStatement = currentStatement match {
      case JsExpStatement(JsFunc(_, _, _)) => true
      case _ => false
    }

    val isLastStatement: Boolean = currentStatement.eq(lastStatement)

    if(isFunctionStatement) {
      if(isLastStatement) {
        _newLineOpt()
      } else {
        _newLine()
      }
    } else {
      if(isLastStatement) {
        _semiOpt()
      } else {
        _semi()
      }
      _newLineOpt()
    }
  }

  private[this] def _stringLiteral(value: String)(implicit writer: JsWriter) {
    writer.print(escapeJavaScriptString(value))
  }

  private[this] def _newLine()(implicit writer: JsWriter) {
    writer.newLine()
  }

  private[this] def _newLineOpt()(implicit writer: JsWriter) {
    writer.newLineOpt()
  }

  private[this] def _space()(implicit writer: JsWriter) {
    writer.print(' ')
  }

  private[this] def _spaceOpt()(implicit writer: JsWriter) {
    writer.printOpt(' ')
  }

  private[this] def _assignment()(implicit writer: JsWriter) {
    writer.print('=')
  }

  private[this] def _blockClose()(implicit writer: JsWriter) {
    writer.popIndent(); writer.print('}'); _newLineOpt()
  }

  private[this] def _blockOpen()(implicit writer: JsWriter) {
    writer.print('{'); writer.pushIndent(); _newLineOpt()
  }

  private[this] def _break()(implicit writer: JsWriter) {
    writer.print(CharsBreak)
  }

  private[this] def _case()(implicit writer: JsWriter) {
    writer.print(CharsCase)
  }

  private[this] def _catch()(implicit writer: JsWriter) {
    writer.print(CharsCatch)
  }

  private[this] def _colon()(implicit writer: JsWriter) {
    writer.print(':')
  }

  private[this] def _continue()(implicit writer: JsWriter) {
    writer.print(CharsContinue)
  }

  private[this] def _debugger()(implicit writer: JsWriter) {
    writer.print(CharsDebugger)
  }

  private[this] def _default()(implicit writer: JsWriter) {
    writer.print(CharsDefault)
  }

  private[this] def _do()(implicit writer: JsWriter) {
    writer.print(CharsDo)
  }

  private[this] def _dot()(implicit writer: JsWriter) {
    writer.print('.')
  }

  private[this] def _else()(implicit writer: JsWriter) {
    writer.print(CharsElse)
  }

  private[this] def _false()(implicit writer: JsWriter) {
    writer.print(CharsFalse)
  }

  private[this] def _finally()(implicit writer: JsWriter) {
    writer.print(CharsFinally)
  }

  private[this] def _for()(implicit writer: JsWriter) {
    writer.print(CharsFor)
  }

  private[this] def _function()(implicit writer: JsWriter) {
    writer.print(CharsFunction)
  }

  private[this] def _if()(implicit writer: JsWriter) {
    writer.print(CharsIf)
  }

  private[this] def _in()(implicit writer: JsWriter) {
    writer.print(CharsIn)
  }

  private[this] def _lbrace()(implicit writer: JsWriter) {
    writer.print('{')
  }

  private[this] def _lparen()(implicit writer: JsWriter) {
    writer.print('(')
  }

  private[this] def _lbrack()(implicit writer: JsWriter) {
    writer.print('[')
  }

  private[this] def _rbrace()(implicit writer: JsWriter) {
    writer.print('}')
  }

  private[this] def _rparen()(implicit writer: JsWriter) {
    writer.print(')')
  }

  private[this] def _rbrack()(implicit writer: JsWriter) {
    writer.print(']')
  }

  private[this] def _ident(identifier: JsIdentifier)(implicit writer: JsWriter) {
    writer.print(identifier.name.name)
  }

  private[this] def _nestedPop(statement: JsStatement)(implicit writer: JsWriter) =
    statement match {
      case _: JsBlock ⇒ false
      case _ ⇒
        writer.popIndent()
        true
    }

  private[this] def _nestedPush(statement: JsStatement, needSpace: Boolean)(implicit writer: JsWriter) =
    statement match {
      case _: JsBlock ⇒
        _spaceOpt()
        false

      case _ ⇒
        if(needSpace) {
          _space()
        }
        writer.pushIndent()
        _newLineOpt()
        true
    }

  private[this] def _new()(implicit writer: JsWriter) {
    writer.print(CharsNew)
  }

  private[this] def _null()(implicit writer: JsWriter) {
    writer.print(CharsNull)
  }

  private[this] def _parenCalc(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean)(implicit writer: JsWriter) = {
    val parentPrec = JsPrecedenceVisitor(parent)
    val childPrec = JsPrecedenceVisitor(child)

    parentPrec > childPrec || (parentPrec == childPrec && wrongAssoc)
  }

  private[this] def _parenPop(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean)(implicit writer: JsWriter) = {
    val doPop = _parenCalc(parent, child, wrongAssoc)

    if(doPop) {
      _rparen()
    }

    doPop
  }

  private[this] def _parenPopIfCommaExpr(expression: JsExpression)(implicit writer: JsWriter) =
    expression match {
      case JsNop(binOp: JsBinary) if binOp.op == JsBinops.`,` ⇒
        _rparen()
        true
      case binOp: JsBinary if binOp.op == JsBinops.`,` ⇒
        _rparen()
        true
      case _ ⇒
        false
    }

  private[this] def _parenPopOrSpace(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean)(implicit writer: JsWriter) = {
    val doPop = _parenCalc(parent, child, wrongAssoc)

    if(doPop) {
      _rparen()
    } else {
      _space()
    }

    doPop
  }

  private[this] def _parenPush(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean)(implicit writer: JsWriter) = {
    val doPush = _parenCalc(parent, child, wrongAssoc)

    if(doPush) {
      _lparen()
    }

    doPush
  }

  private[this] def _parenPushIfCommaExpr(expression: JsExpression)(implicit writer: JsWriter) =
    expression match {
      case JsNop(binOp: JsBinary) if binOp.op == JsBinops.`,` ⇒
        _lparen()
        true
      case binOp: JsBinary if binOp.op == JsBinops.`,` ⇒
        _lparen()
        true
      case _ ⇒
        false
    }

  private[this] def _parenPushOrSpace(parent: JsExpression, child: JsExpression, wrongAssoc: Boolean)(implicit writer: JsWriter) = {
    val doPush = _parenCalc(parent, child, wrongAssoc)

    if(doPush) {
      _lparen()
    } else {
      _space()
    }

    doPush
  }

  private[this] def _questionMark()(implicit writer: JsWriter) {
    writer.print('?')
  }

  private[this] def _return()(implicit writer: JsWriter) {
    writer.print(CharsReturn)
  }

  private[this] def _semi()(implicit writer: JsWriter) {
    writer.print(';')
  }

  private[this] def _semiOpt()(implicit writer: JsWriter) {
    writer.printOpt(';')
  }

  private[this] def _sepCommaOptSpace(sep: Boolean)(implicit writer: JsWriter) {
    if(sep) {
      writer.print(',')
      _spaceOpt()
    }
  }

  private[this] def _slash()(implicit writer: JsWriter) {
    writer.print('/')
  }

  private[this] def _spaceCalc(op: JsOperator, expression: JsExpression)(implicit writer: JsWriter): Boolean =
    if(op.isKeyword) {
      true
    } else {
      expression match {
        case JsBinary(left, binOp, right) if binOp.precedence > op.precedence => _spaceCalc(op, left)
        case JsBinary(_, _, _) => false
        case JsPrefix(preOp, prefixExp) =>
          (op == JsBinops.`-` || op == JsUnops.`-`) &&
          (preOp == JsUnops.`--` || preOp == JsUnops.`-`) ||
          (op == JsBinops.`+` && preOp == JsUnops.`++`)
        case x @ JsNumber(value) =>
          (op == JsBinops.`-` || op == JsUnops.`-`) && x.numberOps.toDouble(value) < 0.0
        // now with JsNop:
        case JsNop(JsBinary(left, binOp, right)) if binOp.precedence > op.precedence => _spaceCalc(op, left)
        case JsNop(JsBinary(_, _, _)) => false
        case JsNop(JsPrefix(preOp, prefixExp)) =>
          (op == JsBinops.`-` || op == JsUnops.`-`) &&
          (preOp == JsUnops.`--` || preOp == JsUnops.`-`) ||
          (op == JsBinops.`+` && preOp == JsUnops.`++`)
        case JsNop(x @ JsNumber(value)) =>
          (op == JsBinops.`-` || op == JsUnops.`-`) && x.numberOps.toDouble(value) < 0.0
        case _ => false
      }
    }

  private[this] def _switch()(implicit writer: JsWriter) {
    writer.print(CharsSwitch)
  }

  private[this] def _this()(implicit writer: JsWriter) {
    writer.print(CharsThis)
  }

  private[this] def _throw()(implicit writer: JsWriter) {
    writer.print(CharsThrow)
  }

  private[this] def _true()(implicit writer: JsWriter) {
    writer.print(CharsTrue)
  }

  private[this] def _try()(implicit writer: JsWriter) {
    writer.print(CharsTry)
  }

  private[this] def _var()(implicit writer: JsWriter) {
    writer.print(CharsVar)
  }

  private[this] def _while()(implicit writer: JsWriter) {
    writer.print(CharsWhile)
  }
}