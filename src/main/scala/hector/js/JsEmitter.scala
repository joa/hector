package hector.js

import java.io.{Writer, StringWriter}

import hector.util.escapeJavaScriptString

/**
 */
object JsEmitter {
  // The JsEmitter is a quick and dirty solution and could be enhanced a whole lot. For instance
  // it is not necessary to put parenthesis around each expression.

  //XXX(joa): replace with proper emitter, maybe re-use gwt code?

  def toNode(ast: JsAST, humanReadable: Boolean = false) = {
    import scala.xml.Unparsed

    <script type="text/javascript">{
      Unparsed(data = toString(ast, humanReadable))
    }</script>
  }

  def toString(ast: JsAST, humanReadable: Boolean = false) = {
    val stringWriter = new StringWriter(0x100)
    implicit val writer = new JsWriter(stringWriter, humanReadable)

    write(ast)

    writer.flush()
    writer.close()
    stringWriter.toString
  }

  private[this] def write(ast: JsAST)(implicit writer: JsWriter) {
    import writer.{pushIndent, popIndent, withIndent, indentOnce, print, println, printIndent}

    ast match {
      // Statements

      case JsProgram(statements) ⇒ statements foreach write

      case JsEmptyStatement ⇒ print(";")

      case JsBlock(statements) ⇒
        withIndent("{", "}") { statements foreach write }

      case JsExpStatement(exp) ⇒
        printIndent()
        print("(")
        write(exp)
        print(");")

      case JsIf(test, trueCase, falseCase) ⇒
        printIndent()
        print("if(")
        write(test)
        println(")")

        indentOnce(trueCase) {
          write(trueCase)
        }

        falseCase foreach {
          stmt ⇒
            println("else")
            indentOnce(stmt) {
              write(stmt)
            }
        }

      case JsLabeledStatement(label, body) ⇒
        printIndent()
        write(label)
        println(":")
        indentOnce(body) {
          write(body)
        }

      case JsBreak(label) ⇒
        writeOptional("break", label)

      case JsContinue(label) ⇒
        writeOptional("continue", label)

      case JsWith(obj, body) ⇒
        printIndent()
        print("with(")
        write(obj)
        println(")")
        indentOnce(body) {
          write(body)
        }

      //TODO(joa): case class JsSwitch(test: JsExpression, cases: Seq[JsSwitchCase])
      case JsReturn(value) ⇒
        writeOptional("return", value)

      case JsThrow(value) ⇒
        printIndent()
        print("throw ")
        write(value)
        println(";")

      case JsWhile(test, body) ⇒
        printIndent()
        print("while(")
        write(test)
        println(")")
        indentOnce(body) {
          write(body)
        }
      case JsDoWhile(test,  body) ⇒
        printIndent()
        println("do")
        indentOnce(body) {
          write(body)
        }
        printIndent()
        print("while(")
        write(test)
        println(");")

      case JsFor(init, test, update, body) ⇒
        printIndent()
        print("for(")
        init foreach write //TODO(joa): turn new lines off, switch ctx
        print(";")
        test foreach write
        print(";")
        update foreach write
        println(")")

        indentOnce(body) {
          write(body)
        }
      case JsForIn(left, right,  body) ⇒
        printIndent()
        print("for(")
        write(left) //TODO(joa): turn new lines off, switch ctx
        print(" in ")
        write(right)
        println(")")

        indentOnce(body) {
          write(body)
        }

      //TODO(joa): case class JsCatchStmt(body: JsStatement) extends JsStatement
      //TODO(joa): case class JsTry(body: JsBlock, handlers: Seq[JsCatchStmt], fin: Option[JsStatement]) extends JsStatement
      case JsVar(name, init) ⇒
        printIndent()
        print("var ")
        write(name)
        init match {
          case Some(value) ⇒
            print(" = ")
            write(value)
            println(";")
          case None ⇒
            println(";")
        }


      case JsVars(vars) ⇒
        printIndent()
        print("var ")

        var comma = ""

        for {
          v ← vars
        } {
          print(comma)
          write(v.name)
          v.init match {
            case Some(value) ⇒
              print(" = ")
              write(value)
            case None ⇒
          }
          comma = ", "
        }

        println(";")

      // Expressions

      case JsNop(expr) ⇒ write(expr)

      case JsIdentifier(name) ⇒
        val oldIdentifier = name.name
        val newIdentifier = new StringBuilder(oldIdentifier.length)

        val n = oldIdentifier.length
        var i =
          if(Character.isJavaIdentifierStart(oldIdentifier.charAt(0))) {
            newIdentifier.append(oldIdentifier.charAt(0))

            // The provided identifier started with a valid character so we continue
            // with the rest of the string which starts at index 1.
            1
          } else {
            newIdentifier.append("_")

            // The provided identifier did not start with a valid character (a number for instance)
            // so we perform the normal conversion for the complete string.
            0
          }

        while(i < n) {
          val character = oldIdentifier.charAt(i)
          newIdentifier.append(if(Character.isJavaIdentifierPart(character)) character else '_')
          i += 1
        }

        print(newIdentifier.toString())

      case JsThis ⇒ print("this")

      case JsArrayAccess(array, index) ⇒
        write(array)
        print("[")
        write(index)
        print("]")

      case JsObj(properties) ⇒
        withIndent("{", "}") {
          var isNotFirst = false

          for { property ← properties } {
            if(isNotFirst) {
              print(",")
            }

            // TODO(joa): getter/setter handling
            write(property.key)
            println(":")

            pushIndent()
            write(property.value)
            popIndent()

            isNotFirst = true
          }
        }

      case JsFunc(id, params, body) ⇒
        print("(function ")
        id foreach write
        print("(")

        var isNotFirst = false

        for { param ← params } {
          if(isNotFirst) {
            print(",")
          }

          write(param)

          isNotFirst = true
        }

        println(")")
        write(body)
        println(")")

      case JsSeq(exp) ⇒
        var isNotFirst = false
        for { e ← exp } {
          if(isNotFirst) {
            print(",")
          }

          write(e)

          isNotFirst = true
        }

      case JsUnary(op, value) ⇒
        import JsUnops._

        print(op match {
          case `-` ⇒ "-"
          case `+` ⇒ "+"
          case `!` ⇒ "!"
          case `~` ⇒ "~"
          case `typeof` ⇒ "typeof"
          case `void` ⇒ "void"
          case `delete` ⇒ "delete"
        })

        write(value)

      case JsBinary(left, op, right) ⇒
        import JsBinops._

        print("(")
        write(left)

        print(op match {
          case `==` ⇒ "=="
          case `!=` ⇒ "!="
          case `===` ⇒ "==="
          case `!==` ⇒ "!=="
          case `<` ⇒ "<"
          case `<=` ⇒ "<="
          case `>` ⇒ ">"
          case `>=` ⇒ ">="
          case `<<` ⇒ "<<"
          case `>>` ⇒ ">>"
          case `>>>` ⇒ ">>>"
          case `+` ⇒ "+"
          case `-` ⇒ "-"
          case `*` ⇒ "*"
          case `/` ⇒ "/"
          case `%` ⇒ "%"
          case `|` ⇒ "|"
          case `^` ⇒ "^"
          case `in` ⇒ " in "
          case `instanceof` ⇒ " instanceof "
          case `||`  ⇒ "||"
          case `&&` ⇒ "&&"
        })

        write(right)
        print(")")

      case JsAssignment(left, op, right) ⇒
        import JsAssignmentOps._

        print("(")
        write(left)

        println(op match {
          case `=` ⇒ "="
          case `+=` ⇒ "+="
          case `-=` ⇒ "-="
          case `*=` ⇒ "*="
          case `/=` ⇒ "/="
          case `%=` ⇒ "%="
          case `<<=` ⇒ "<<="
          case `>>=` ⇒ ">>="
          case `>>>=` ⇒ ">>>="
          case `|=` ⇒ "|="
          case `^=` ⇒ "^="
          case `&=` ⇒ "&="
        })

        write(right)
        print(")")

      case JsPreInc(exp) ⇒
        print("++")
        write(exp)

      case JsPostInc(exp) ⇒
        write(exp)
        print("++")

      case JsPreDec(exp) ⇒
        print("--")
        write(exp)

      case JsPostDec(exp) ⇒
        write(exp)
        print("--")

      case JsCondition(test, trueCase, falseCase) ⇒
        print("((")
        write(test)
        print(")?")
        write(trueCase)
        print(":")
        write(falseCase)
        print(")")

      case JsNew(constructor, arguments) ⇒
        print("new ")
        write(constructor)

        print("(")

        var isNotFirst = false

        for { arg ← arguments } {
          if(isNotFirst) {
            print(",")
          }

          write(arg)

          isNotFirst = true
        }

        print(")")

      case JsCall(callee, arguments) ⇒
        write(callee)

        print("(")

        var isNotFirst = false

        for { arg ← arguments } {
          if(isNotFirst) {
            print(",")
          }

          write(arg)

          isNotFirst = true
        }

        print(")")

      case JsMember(obj, property) ⇒
        write(obj)
        print(".")
        write(property)

      case JsString(value) ⇒
        print(escapeJavaScriptString(value))

      case JsArray(elements) ⇒
        print("[")

        var isNotFirst = false

        for { element ← elements } {
          if(isNotFirst) {
            print(",")
          }

          write(element)
          isNotFirst = true
        }

        print("]")

      case JsTrue ⇒
        print("true")

      case JsFalse ⇒
        print("false")

      case JsNull ⇒
        print("null")

      case JsNumber(value) ⇒
        print(value.toString)

      //TODO(joa): case JsRegEx(value) ⇒ print(value)

      case JsXML(value) ⇒
        print(value.toString())
    }
  }

  private def writeOptional(keyword: String, value: Option[JsAST])(implicit writer: JsWriter) {
    import writer.{print, println, printIndent}

    value match {
      case Some(identifier) ⇒
        printIndent()
        print(keyword+" ")
        write(identifier)
        println(";")
      case None ⇒
        println(keyword+";")
    }
  }
}

private final class JsWriter(writer: Writer, humanReadable: Boolean) extends Writer {
  @volatile private[this] var indent = 0
  private[this] val tabs = Array("", "  ", "    ", "      ", "        ", "          ")

  def pushIndent() {
    indent = math.min(indent + 1, tabs.length)
  }

  def popIndent() {
    indent = math.max(indent - 1, 0)
  }

  /**
   * Only perform indentation if the test subject is not a statement list.
   *
   * @param test The AST node to test.
   * @param f The function to call.
   * @tparam U The return type of f.
   */
  def indentOnce[U](test: JsAST)(f: ⇒ U) {
    if(humanReadable) {
      test match {
        case _: JsBlock ⇒ f
        case _ ⇒
          pushIndent()
          f
          popIndent()
      }
    } else {
      f
    }
  }

  def withIndent[U](before: String, after: String)(f: ⇒ U) {
    println(before)
    pushIndent()
    f
    popIndent()
    println(after)
  }

  def printIndent() {
    write(tabs(indent))
  }

  def print(output: String) {
    write(output)
  }

  def println(line: String) {
    if(humanReadable) {
      write(tabs(indent)+line+"\n")
    } else {
      write(line)
    }
  }

  def write(charBuffer: Array[Char], offset: Int, length: Int) {
    writer.write(charBuffer, offset, length)
  }

  def flush() {
    writer.flush()
  }

  def close() {
    writer.close()
  }
}
