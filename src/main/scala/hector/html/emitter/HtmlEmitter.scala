package hector.html.emitter

import hector.html._
import hector.util.trimToOption

import javax.annotation.Nullable
import javax.annotation.concurrent.ThreadSafe
import java.io.{PrintWriter, StringWriter}

import scala.xml.{NamespaceBinding, MetaData, Node}
import scala.collection.immutable.Stack

/**
 */
@ThreadSafe
object HtmlEmitter {
  private[this] val CharsCommentOpen = "<!--".toCharArray

  private[this] val CharsCommentClose = "-->".toCharArray

  private[this] val CharsTagClose = "</".toCharArray

  private[this] val CharsCDataOpen = "<![CDATA[".toCharArray

  private[this] val CharsCDataClose = "]]>".toCharArray

  private[this] val CharsProcInstrOpen = "<?".toCharArray

  private[this] val CharsProcInstrClose = "?>".toCharArray

  def toString(html: Node, docType: DocType = DocTypes.`HTML 5`, stripComments: Boolean = false, trim: Boolean = false, humanReadable: Boolean = false, omitDocType: Boolean = false): String = {
    val stringWriter = new StringWriter()
    val printWriter = new PrintWriter(stringWriter)
    val writer = new HtmlWriter(printWriter, humanReadable)

    if(!omitDocType) {
      _dtd(docType)(writer)
    }

    visit(html, docType, Stack.empty, stripComments, trim, humanReadable)(writer)

    printWriter.flush()
    printWriter.close()

    stringWriter.toString
  }

  private[this] def visit(node: Node, docType: DocType, scopeStack: Stack[NamespaceBinding], stripComments: Boolean, trim: Boolean, humanReadable: Boolean)(implicit writer: HtmlWriter) {
    import scala.xml._

    // Subsequent whitespace could be removed. This should be something we have to consider since
    // it will generate an Html output that is much easier on the eye when looking at the source.

    node match {
      case Comment(text) ⇒
        _commentOpen()
        _string(text, trim)
        _commentClose()
        _newLineOpt()

      case elem: Elem ⇒
        // Note: Using a pattern match like one would expect, e.g. case Elem(prefix, label, attributes, scope, children)
        // will lead to a MatchError.

        @Nullable val prefix = elem.prefix
        val label = elem.label
        @Nullable val attributes = elem.attributes
        val scope = elem.scope
        val children = elem.child

        if(children.isEmpty) {
          _lt()
          _tag(prefix, label, attributes, scope, scopeStack, docType, stripComments, trim, humanReadable)
          if(docType != DocTypes.`HTML 5` && docType != DocTypes.`XHTML 5`) {
            _tagCloseShort()
          } else {
            _gt()
          }
        } else {
          _lt()
          _tag(prefix, label, attributes, scope, scopeStack, docType, stripComments, trim, humanReadable)
          _gt()

          _newLineOpt()
          writer.pushIndent()

          val newScopeStack = if(scopeStack.contains(scope)) scopeStack else scopeStack.push(scope)

          children foreach {
            child ⇒
              visit(child, docType, newScopeStack, stripComments, trim, humanReadable)
          }
          _tagCloseLong(prefix, label)
        }
        _newLineOpt()

      case EntityRef(name) ⇒
        _entity(name)

      case Group(nodes) ⇒
        nodes foreach {
          node ⇒ visit(node, docType, scopeStack, stripComments, trim, humanReadable)
        }

      case PCData(data) ⇒
        // So this is <![CDATA[data]]> but not PCData because PCData is Atom apparently?!
        _cdataOpen()
        writer.print(data)
        _cdataClose()

      case ProcInstr(target, text) ⇒
        _procInstrOpen()
        writer.print(target)
        trimToOption(text) match {
          case Some(textValue) ⇒
            _space()
            writer.print(textValue)
          case None ⇒
        }
        _procInstrClose()

      case Text(value) ⇒
        _string(value, trim)

      case Unparsed(data) ⇒
        writer.print(data)

      case atom: Atom[_] ⇒
        // Apparently someone decided to name PCDATA not PCDATA but Atom.
        // So we will have to treat it like parsed character data.
        _string(atom.data.toString, trim)
    }
  }

  private[this] def _dtd(docType: DocType)(implicit writer: HtmlWriter) {
    writer.print(docType.charArray)
    _newLine()
  }

  private[this] def _tag(@Nullable prefix: String, label: String, @Nullable attributes: MetaData, scope: NamespaceBinding, scopeStack: Stack[NamespaceBinding], docType: DocType, stripComments: Boolean, trim: Boolean, humanReadable: Boolean)(implicit writer: HtmlWriter) {
    if(null != prefix) {
      _prefix(prefix)
      _colon()
    }

    _name(label)

    if(null != attributes) {
      for { maybeAnAttribute ← attributes } {
        import scala.xml._

        maybeAnAttribute match {
          case Null ⇒
          case attribute: Attribute ⇒
            @Nullable val prefix = attribute.pre
            val label = attribute.key
            @Nullable val value = attribute.value

            _space()

            if(null != prefix) {
              _prefix(prefix)
              _colon()
            }

            _name(label)

            if(null != value) {
              writer.print('=')
              writer.print('"')

              if(value forall { _.isInstanceOf[Text] }) {
                // The common case: All attribute elements are text nodes. Who would have
                // thought about that?!

                // This is an evil side-effect we are willing to introduce into our beautiful
                // map operation so we do not have to call (stringSequence map { _.length }).sum
                // which makes us traverse the sequence again.
                var totalLength = 0

                val stringSequence: Seq[String] = value map {
                  textNode ⇒
                    val result = textNode.asInstanceOf[Text].data
                    totalLength += result.length
                    result
                }

                // Use "totalLength" instead of "(stringSequence map { _.length }).sum" here.
                val stringBuilder = new StringBuilder(totalLength)

                stringSequence foreach stringBuilder.append

                // Never trim any attributes because this is absolutely the decision of the
                // person generating an attribute in the first place.

                _string(stringBuilder.toString(), trim = false)
              } else {
                // Whatever this is ...

                value foreach {
                  element ⇒
                    //TODO(joa): remove before going into prod
                    println("#####################################################################")
                    println("# JACKPOT: "+element)
                    println("#####################################################################")

                    // However we do not perform any additional escaping since toString(...) will
                    // already contain an escaped sequence. But we might want to look for
                    // any closing XML tag

                    //TODO(joa): escape closing XML tags
                    writer.print(toString(element, docType, stripComments, trim = false, humanReadable = humanReadable, omitDocType = true))
                }
              }

              writer.print('"')
            }
        }
      }
    }


    //TODO(joa): how did he implement namespaces?! its quite weird ...
    //scope
  }

  private[this] def _tagCloseLong(@Nullable prefix: String, label: String)(implicit writer: HtmlWriter) {
    writer.print(CharsTagClose)

    if(null != prefix) {
      _prefix(prefix)
      _colon()
    }

    _name(label)
    writer.print('>')

    writer.popIndent()
  }

  private[this] def _tagCloseShort()(implicit writer: HtmlWriter) {
    writer.print('/')
    _gt()
  }

  private[this] def _entity(name: String)(implicit writer: HtmlWriter) {
    _and()
    writer.print(name) //TODO(joa): check for valid entity name
    _semi()
  }

  private[this] def _prefix(prefix: String)(implicit writer: HtmlWriter) {
    if(isName(prefix)) {
      writer.print(prefix)
    } else {
      writer.print("invalidName")
    }
  }

  private[this] def _name(name: String)(implicit writer: HtmlWriter) {
    if(isName(name)) {
      writer.print(name)
    } else {
      writer.print("invalidName")
    }
  }

  private[this] def _cdataOpen()(implicit writer: HtmlWriter) {
    writer.print(CharsCDataOpen)
  }

  private[this] def _cdataClose()(implicit writer: HtmlWriter) {
    writer.print(CharsCDataClose)
    writer.newLineOpt()
  }

  private[this] def _procInstrOpen()(implicit writer: HtmlWriter) {
    writer.print(CharsProcInstrOpen)
  }

  private[this] def _procInstrClose()(implicit writer: HtmlWriter) {
    writer.print(CharsProcInstrClose)
    writer.newLineOpt()
  }

  private[this] def _colon()(implicit writer: HtmlWriter) {
    writer.print(':')
  }

  private[this] def _lt()(implicit writer: HtmlWriter) {
    writer.print('<')
  }

  private[this] def _gt()(implicit writer: HtmlWriter) {
    writer.print('>')
  }

  private[this] def _commentOpen()(implicit writer: HtmlWriter) {
    writer.print(CharsCommentOpen)
    _spaceOpt()
  }

  private[this] def _commentClose()(implicit writer: HtmlWriter) {
    _spaceOpt()
    writer.print(CharsCommentClose)
  }

  private[this] def _newLine()(implicit writer: HtmlWriter) {
    writer.newLine()
  }

  private[this] def _newLineOpt()(implicit writer: HtmlWriter) {
    writer.newLineOpt()
  }

  private[this] def _space()(implicit writer: HtmlWriter) {
    writer.print(' ')
  }

  private[this] def _spaceOpt()(implicit writer: HtmlWriter) {
    writer.printOpt(' ')
  }

  private[this] def _string(value: String, trim: Boolean)(implicit writer: HtmlWriter) {
    //TODO(joa): use proper escape method ...
    writer.print(xml.Utility.escape(if(trim) trimHtmlText(value) else value))
  }

  private[this] def _and()(implicit writer: HtmlWriter) {
    writer.print('&')
  }

  private[this] def _semi()(implicit writer: HtmlWriter) {
    writer.print(';')
  }

  /**
   * trimHtmlText tries to trim an arbitrary Html text node.
   *
   * <p>Note that it is not possible to simply trim any content because "&lt;span&gt;foo &lt;/span&gt;bar"
   * would look like "foobar" if we simply trim any text node. Therefore we have two options:</p>
   * <ul>
   *   <li>Trim the text keeping the first and/or last whitespace character</li>
   *   <li>Trim the text, keep only the necessary first and/or last whitespace character</li>
   * </ul>
   *
   * <p>Since it is not trivial to determine whether or not a whitespace character is required we
   * will go with the first option for now.</p>
   *
   * @param value The text to trim.
   *
   * @return The trimmed value; may still start or end with a whitespace character.
   */
  private[this] def trimHtmlText(value: String): String = {
    val length = value.length

    if(null != value) {
      if(length == 1) {
        // Make no attempt to trim a single character because it could be \n, \x32 or anything
        // else and we do want to preserve single whitespace.
        value
      } else {
        val firstChar = value.charAt(0)
        val lastChar = value.charAt(length - 1)

        value match {
          case startsAndEndsWithWhitespace if Character.isWhitespace(firstChar) && Character.isWhitespace(lastChar) ⇒
            firstChar+startsAndEndsWithWhitespace.trim+lastChar

          case startsWithWhitespace if Character.isWhitespace(firstChar) ⇒
            firstChar+startsWithWhitespace.trim

          case endsWithWhitespace if Character.isWhitespace(lastChar) ⇒
            endsWithWhitespace.trim+lastChar

          case ordinaryString ⇒
            ordinaryString
        }
      }
    } else if(null == value) {
      // Catch any null values and return "" instead.
      ""
    }  else {
      // Empty string.
      value
    }
  }

  // The isName method is not used correct since prefix:label makes a name and Elem
  // does not define it as such. For now we do it like isName(prefix):isName(label)
  // so in fact the "label" part of Elem is also checked for isNameStart but in the end
  // it is only making sure that a name is correct even tough label is null.

  private[this] def isName(value: String): Boolean =
    testStringForValidity(value, isNameStart, isNamePart)

  /**
   * http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
   * @param char
   * @return
   */
  private[this] def isNameStart(char: Char): Boolean =
    char match {
      case letter if Character.isLetter(letter) ⇒ true
      case '_' | ':' ⇒ true
      case _ ⇒ false
    }

  /**
   * http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
   *
   * @param char
   * @return
   */
  private[this] def isNamePart(char: Char): Boolean =
    char match {
      case letter if Character.isLetter(letter) ⇒ true
      case digit if Character.isDigit(digit) ⇒ true
      case '.' | '-' | '_' | ':' ⇒ true
      //case Extender ⇒ true
      //case Combiner ⇒ true
    }

  private[this] def testStringForValidity(value: String, startIsValid: Char => Boolean, partIsValid: Char => Boolean): Boolean = {
    if(null == value || value.length < 1) {
      false
    } else {
      val charArray = value.toCharArray

      if(startIsValid(charArray(0))) {
        val n = value.length
        var i = 1
        var valid = true

        while(valid && i < n) {
          valid = partIsValid(charArray(i))
          i += 1
        }

        valid
      } else {
        false
      }
    }
  }
}
