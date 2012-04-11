package hector.html.emitter

import com.google.common.collect.ImmutableSortedSet

import hector.html._
import hector.util.{TextOutput, trimToOption}

import javax.annotation.Nullable
import javax.annotation.concurrent.ThreadSafe

import scala.xml._

/**
 * The HtmlEmitter is used to emit a string for a given Node.
 *
 * <p>The actual implementation will make sure that:
 *   <ul>
 *     <li>Text is escaped.</li>
 *     <li>Output contains only of valid characters. Invalid data is ignored.</li>
 *     <li>Consecutive whitespace is trimmed</li>
 *     <li>Invalid tag names are replaced with "invalidName".</li>
 *     <li>Invalid attribute names are replaced with "invalidName".</li>
 *     <li>Invalid prefix names are replaced with "invalidName".</li>
 *     <li>Output contains only valid entity references. Invalid data is ignored.</li>
 *     <li>Output is formatted according to given DTD.</li>
 *   </ul>
 * </p>
 *
 * <p>An invalid tag name would be <code>Elem("foo bar", ...)</code> where "foo bar" is an
 * invalid name. The same applies for attribute values and prefixes. Although when using
 * XML literals it is impossible to create such malformed data it is an extra security check
 * in order to prevent mistakes like <code>Elem(loadSomethingFromDB(), ...)</code>.</p>
 *
 * <p>The HtmlEmitter is also very strict when it comes to entity references such as &amp;amp. If
 * a given entity name is not known or not of the form &amp;#UUUU; or &amp;#xHHHH; it is ignored.</p>
 *
 * <p>Furthermore the HtmlEmitter can pretty-print any data with little impact on performance. Microbenchmark
 * scores using real-world data suggest that using <code>stripComments</code> and <code>trim</code>
 * have little to no performance impact. In fact using <code>trim</code> might increase performance
 * since less data needs to be checked for escaping.</p>
 *
 * <p>When using <code>trim</code> and <code>humanReadable</code> the HtmlEmitter will generate
 * pretty html output that is easy on the eye, ignoring any indentation in the actual Scala code.</p>
 */
@ThreadSafe
object HtmlEmitter {
  //
  // Note:
  //   Please do not try to replace the while-loops in this code with a foreach-equivalent.
  //   HtmlEmitter may contain code duplicates (in _string for instance) which are only there
  //   in order to improve performance and not have any unnecessary object allocations.
  //
  //   If you make any changes to this file you must run the HtmlEmitterBenchmark for it.
  //   To do so you can use "hector-microbenchmark/run" from within SBT.
  //
  //   Also check the generated bytecode with "javap -c -p ..." and make sure that no object
  //   allocations appear in any "_method" or "visit".
  //
  //   You might also be surprised about the fact that _string performs actual escaping and
  //   that we do not rely on

  private[this] val CharsCommentOpen = "<!--".toCharArray

  private[this] val CharsCommentClose = "-->".toCharArray

  private[this] val CharsTagClose = "</".toCharArray

  private[this] val CharsCDataOpen = "<![CDATA[".toCharArray

  private[this] val CharsCDataClose = "]]>".toCharArray

  private[this] val CharsProcInstrOpen = "<?".toCharArray

  private[this] val CharsProcInstrClose = "?>".toCharArray

  private[this] val CharsEscapedLt = "&lt;".toCharArray

  private[this] val CharsEscapedGt = "&gt;".toCharArray

  private[this] val CharsEscapedAmp = "&amp;".toCharArray

  private[this] val CharsEscapedQuot = "&quot;".toCharArray

  /**
   * Creates and returns a String for the given node.
   *
   * <p>If <code>trim</code> is set to <code>true</code> there are some very
   * important things to consider. It is not possible to trim white-space
   * correct ahead of time. It is impossible to know whether or
   * not whitespace needs to be preserved since user-code might contain
   * JavaScript which would set "white-space: pre" for some element.</p>
   *
   * <p>Hector tries to be correct when collapsing whitespace. That means
   * only consecutive whitespace is collapsed to the left-most tag. If you do
   * not use any white-space related CSS property (like "white-space") you
   * will not get any problems. Hector will not trim text inside &lt;pre&gt;
   * elements.</p>
   *
   * <p>The actual white-space collapse algorithm depends on the <code>humanReadable</code>
   * flag. If <code>humanReadable</code> is set to <code>false</code> a cheap
   * trimming algorithm is used which will collapse only consecutive whitespace in a
   * single tag preserving the first and/or last whitespace character. This is a
   * very cheap way of trimming text while preserving correct output.</p>
   * 
   * <p>Hector will treat <code>Unparsed</code> data like a normal Html node when
   * consecutive whitespace needs to be detected.</p>
   *
   * @param html The root of the document.
   * @param docType The DTD to use. 
   * @param stripComments Whether or not to strip &lt;!-- comments --&gt;
   * @param trim Whether or not to trim strings.
   * @param humanReadable Whether or not to create well formatted source code.
   */
  def toString(html: Node, docType: DocType = DocTypes.`HTML 5`, stripComments: Boolean = false, trim: Boolean = false, humanReadable: Boolean = false): String = {
    //
    // See https://developers.google.com/speed/articles/web-metrics
    //
    // I also read that the average character data is at 20k bytes but I am missing the
    // source for that information. However 20.000 lines up closely to other observed pages.
    // You can try it via "curl http://... | wc -c"
    //
    // This size should be part of a configuration with the default being 20k.
    //
    val stringBuilder = new StringBuilder(20000) //TODO(joa): make me configurable.
    val writer = new TextOutput(stringBuilder, humanReadable)

    //val typeBuffer = new scala.collection.mutable.ArrayBuffer[Int](0x200)
    //calculateWhitespace(html, typeBuffer)

    // Whitespace is collapsed to the left.
    //
    // In the following conditions the whitespace is not collapsed:
    //    <pre>-context
    //    "white-space: pre" via CSS
    //
    // There are also other tags which clear the collapse behaviour.
    // Specifically <br/>, <p> and other elements leading to a page break.
    // However this can depend entirely on CSS too.
    //
    // <b>foo</b> <i>bar</i><br/>      {foo}{ }{bar}
    // <b>foo </b> <i>bar</i><br/>     {foo }{bar}
    // <b>foo</b> <i> bar</i><br/>     {foo}{ }{bar}
    // <b>foo </b> <i> bar</i><br/>    {foo }{bar}
    //
    // Note that any whitespace immediately following an opening tag is ignored.
    // Note that any whitespace immediately preceeding a closing tag is ignored.
    //

    val trimType = 
      if(trim) {
        if(humanReadable) {
          // Using the pretty/expensive trim method
          2
        } else {
          // Using "ghetto"-trimming™ since no beautiful output is intended.
          1
        }
      } else {
        0
      }

    _dtd(docType)(writer)

    visit(html, docType, stripComments, trim, preDepth = 0, afterOpen = true, beforeClose = true)(writer)

    stringBuilder.toString()
  }

  private[this] def calculateWhitespace(node: Node, types: scala.collection.mutable.ArrayBuffer[Int]) {
    node match {
      case Text(value) ⇒
        val chars = value.toCharArray
        val n = value.length
        var i = 0
        var isWhitespaceOnly = true

        while(i < n && isWhitespaceOnly) {
          isWhitespaceOnly = chars(i) match {
            case ' ' | '\r' | '\n' | '\t' ⇒ true
            case _ ⇒ false
          }

          i += 1
        }

        // 0 = whitespace only
        // 1 = text only
        // 2 = text, begins with whitespace
        // 3 = text, ends with whitespace
        // 4 = text, begins and ends with whitespace

        if(isWhitespaceOnly) {
          // " "
          types += 0
        } else {
          // It is safe to assume chars is not empty since otherwise isOnlyWhitespace would
          // be set to true.

          types +=
            (chars(0) match {
              case ' ' | '\r' | '\n' | '\t' ⇒
                chars(n - 1) match {
                  case ' ' | '\r' | '\n' | '\t' ⇒
                    //" foo "
                    4
                  case _ ⇒
                    // " foo"
                    2
                }
              case _ ⇒
                chars(n - 1) match {
                  case ' ' | '\r' | '\n' | '\t' ⇒
                    //"foo "
                    3
                  case _ ⇒
                    //"foo"
                    1
                }
            })
        }

      case elem: Elem ⇒
        val iterator = elem.child.iterator

        while(iterator.hasNext) {
          calculateWhitespace(iterator.next(), types)
        }

      case Group(nodes) ⇒
        val iterator = nodes.iterator

        while(iterator.hasNext) {
          calculateWhitespace(iterator.next(), types)
        }

      case _ ⇒
    }
  }

  /**
   *
   * @param node The node to visit.
   * @param docType The current document type.
   * @param stripComments Whether or not to ignore comments.
   * @param trim Whether or not to trim text.
   * @param preDepth The depth of &lt;pre&gt;-tags
   * @param writer The implicit writer.
   */
  private[this] def visit(node: Node, docType: DocType, stripComments: Boolean, trim: Boolean, preDepth: Int, afterOpen: Boolean, beforeClose: Boolean)(implicit writer: TextOutput) {
    import scala.xml._

    // Subsequent whitespace could be removed. This should be something we have to consider since
    // it will generate an Html output that is much easier on the eye when looking at the source.

    node match {
      case Text(value) ⇒
        _string(value, trim && preDepth == 0, afterOpen, beforeClose)

      case elem: Elem ⇒
        // Note: Using a pattern match like one would expect, e.g. case Elem(prefix, label, attributes, scope, children)
        // will lead to a MatchError.

        @Nullable val prefix = elem.prefix
        val label = elem.label
        @Nullable val attributes = elem.attributes
        val scope = elem.scope
        val children = elem.child
        val isPre = label == "pre" && (null == prefix || prefix == "")

        if(children.isEmpty) {
          _lt()
          _tag(prefix, label, attributes, scope)
          if(docType != DocTypes.`HTML 5` && docType != DocTypes.`XHTML 5`) {
            _tagCloseShort()
          } else {
            _gt()
          }
        } else {
          _lt()
          _tag(prefix, label, attributes, scope)
          _gt()

          //
          // Record whether or not we are in a <pre>-environment.
          // This is important because we may not trim strings if that is the case.
          //
          // If we are not interested in trimming strings, we do not record the preDepth.
          //

          val newPreDepth =
            if(trim && isPre) {
              preDepth + 1
            } else {
              preDepth
            }

          if(!isPre) {
            _newLineOpt()
            writer.pushIndent()
          }

          //TODO(joa): special treatment for first & last since line feed can be ignored

          val iterator = children.iterator

          if(iterator.hasNext) {
            visit(iterator.next(), docType, stripComments, trim, newPreDepth, afterOpen = true, beforeClose = !iterator.hasNext)

            while(iterator.hasNext) {
              visit(iterator.next(), docType, stripComments, trim, newPreDepth, afterOpen = false, beforeClose = !iterator.hasNext)
            }
          }

          _tagCloseLong(prefix, label)
        }

        if(!isPre) {
          _newLineOpt()
        }

      case Group(nodes) ⇒
        val iterator = nodes.iterator

        if(iterator.hasNext) {
          visit(iterator.next(), docType, stripComments, trim, preDepth, afterOpen = afterOpen, beforeClose = beforeClose && !iterator.hasNext)

          while(iterator.hasNext) {
            visit(iterator.next(), docType, stripComments, trim, preDepth, afterOpen = false, beforeClose = beforeClose && !iterator.hasNext)
          }
        }

      case Unparsed(data) ⇒
        writer.print(data)

      case EntityRef(name) ⇒
        _entity(name)

      case PCData(data) ⇒
        //
        // This is <![CDATA[data]]> but not PCData because PCData is Atom apparently
        //
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

      case Comment(text) ⇒
        if(!stripComments) {
          _commentOpen()
          _string(text, trim, afterOpen = true, beforeClose = true)
          _commentClose()
          _newLineOpt()
        }

      case atom: Atom[_] ⇒
        //
        // Apparently someone decided to name PCDATA not PCDATA but Atom.
        // So we will have to treat it like parsed character data.
        //
        _string(atom.data.toString, trim && preDepth == 0, afterOpen, beforeClose)
    }
  }

  @inline
  private[this] def _dtd(docType: DocType)(implicit writer: TextOutput) {
    writer.print(docType.charArray)
    _newLine()
  }

  private[this] def _tag(@Nullable prefix: String, label: String, @Nullable attributes: MetaData, scope: NamespaceBinding)(implicit writer: TextOutput) {
    if(null != prefix) {
      _prefix(prefix)
      _colon()
    }

    _name(label)

    import scala.xml._

    //
    // About the weirdness: MetaData is an iterator in itself. So MetaData.next returns the next
    // element which can be null, Null or an Attribute.
    //

    var iterator = attributes

    while(null != iterator && Null != iterator) {
      //
      // Calling hasNext would lead to an error and instead we must check for Null.
      //
      // Because we tested for Null in the header the cast is safe here.
      //

      val attribute = iterator.asInstanceOf[Attribute]
      iterator = iterator.next

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

        val iterator = value.iterator

        while(iterator.hasNext) {
          _attributeValue(iterator.next())
        }

        writer.print('"')
      }
    }

    //TODO(joa): how did he implement namespaces?! its quite weird ...
    //scope
  }

  private[this] def _attributeValue(node: Node)(implicit writer: TextOutput) {
    node match {
      case text: Text ⇒
        // Never trim any attributes because this is absolutely the decision of the
        // person generating an attribute in the first place.
        _string(text.data, trim = false, afterOpen = false, beforeClose = false)

      case EntityRef(name) ⇒
        _entity(name)

      case Unparsed(data) ⇒
        writer.print(data)

      case Group(group) ⇒
        val iterator = group.iterator
        while(iterator.hasNext) {
          _attributeValue(iterator.next())
        }

      case invalid ⇒ sys.error("Invalid node "+invalid+" in attribute value.")
    }
  }

  private[this] def _tagCloseLong(@Nullable prefix: String, label: String)(implicit writer: TextOutput) {
    writer.print(CharsTagClose)

    if(null != prefix) {
      _prefix(prefix)
      _colon()
    }

    _name(label)
    writer.print('>')

    writer.popIndent()
  }

  private[this] def _tagCloseShort()(implicit writer: TextOutput) {
    writer.print('/')
    _gt()
  }

  private[this] def _entity(name: String)(implicit writer: TextOutput) {
    if(isEntity(name)) {
      _and()
      writer.print(name)
      _semi()
    }
  }

  private[this] def _prefix(prefix: String)(implicit writer: TextOutput) {
    if(isName(prefix)) {
      writer.print(prefix)
    } else {
      writer.print("invalidName")
    }
  }

  private[this] def _name(name: String)(implicit writer: TextOutput) {
    if(isName(name)) {
      writer.print(name)
    } else {
      writer.print("invalidName")
    }
  }

  private[this] def _cdataOpen()(implicit writer: TextOutput) {
    writer.print(CharsCDataOpen)
  }

  private[this] def _cdataClose()(implicit writer: TextOutput) {
    writer.print(CharsCDataClose)
    writer.newLineOpt()
  }

  private[this] def _procInstrOpen()(implicit writer: TextOutput) {
    writer.print(CharsProcInstrOpen)
  }

  private[this] def _procInstrClose()(implicit writer: TextOutput) {
    writer.print(CharsProcInstrClose)
    writer.newLineOpt()
  }

  private[this] def _colon()(implicit writer: TextOutput) {
    writer.print(':')
  }

  private[this] def _lt()(implicit writer: TextOutput) {
    writer.print('<')
  }

  private[this] def _gt()(implicit writer: TextOutput) {
    writer.print('>')
  }

  private[this] def _commentOpen()(implicit writer: TextOutput) {
    writer.print(CharsCommentOpen)
    _spaceOpt()
  }

  private[this] def _commentClose()(implicit writer: TextOutput) {
    _spaceOpt()
    writer.print(CharsCommentClose)
  }

  private[this] def _newLine()(implicit writer: TextOutput) {
    writer.newLine()
  }

  private[this] def _newLineOpt()(implicit writer: TextOutput) {
    writer.newLineOpt()
  }

  private[this] def _space()(implicit writer: TextOutput) {
    writer.print(' ')
  }

  private[this] def _spaceOpt()(implicit writer: TextOutput) {
    writer.printOpt(' ')
  }

  private[this] def _string(value: String, trim: Boolean, afterOpen: Boolean, beforeClose: Boolean)(implicit writer: TextOutput) {
    import writer.print

    val chars = (if(trim) trimHtmlText(value, afterOpen, beforeClose) else value).toCharArray
    val n = chars.length
    var i = 0

    var indexStart = -1

    //
    // The escape method is a little bit special but easy to understand.
    //
    // 1) Print-Characters Only (' ' <= x <= '~'):
    //
    //    Example: ['f', 'o', 'o'].
    //
    //    In the first iteration when looking at 'f' we match a print character and set indexStart
    //    to 0 because it was -1. Now for the following characters 'o' and 'o' nothing happens.
    //
    //    After the loop the range [indexStart, n) will be printed from the sequence of
    //    characters if indexStart is not -1. This will create only one call to
    //    writer.print().
    //
    // 2) Trivial Escape Characters (\n, \r\, \t):
    //
    //    Example: ['f', 'o', 'o', '\n', 'b', 'a', 'r']
    //
    //    In the first iteration when looking at 'f' we set indexStart to 0. Once we encounter
    //    the escape character '\n' we perform no special action since it is valid and has
    //    no escaped HTML entity. We continue processing like in case (1).
    //    Only one call to writer.print() will be made.
    //
    // 3) Non-Trivial Escape Characters (<, >, &, "):
    //
    //    Example: ['f', 'o', 'o', '&', 'b', 'a', 'r']
    //
    //    In the first iteration when looking at 'f' we set indexStart to 0. When we reach the
    //    '&' character the following happens:
    //
    //      - Print all character data in the range [indexStart, i) if indexStart != -1
    //      - Set i to -1 so that the next trivial character will mark it
    //      - Print the HTML entity &amp; instead of &.
    //
    //    This will lead to less calls to writer.print. In fact each escape character requires
    //    an additional 2 calls so we have (1 + 2 * numEntities) calls to writer.print in the worst
    //    case.
    //

    while(i < n) {
      val char = chars(i)

      char match {
        case '<' ⇒
          if(-1 != indexStart) {
            print(chars, indexStart, i - indexStart)
            indexStart = -1
          }

          print(CharsEscapedLt)

        case '>' ⇒
          if(-1 != indexStart) {
            print(chars, indexStart, i - indexStart)
            indexStart = -1
          }

          print(CharsEscapedGt)

        case '&' ⇒
          if(-1 != indexStart) {
            print(chars, indexStart, i - indexStart)
            indexStart = -1
          }

          print(CharsEscapedAmp)

        case '"' ⇒
          if(-1 != indexStart) {
            print(chars, indexStart, i - indexStart)
            indexStart = -1
          }

          print(CharsEscapedQuot)

        case '\n' | '\r' | '\t' ⇒
          if(-1 == indexStart) {
            indexStart = i
          }

        case printChar if ' ' <= printChar && printChar <= '~' ⇒
          if(-1 == indexStart) {
            indexStart = i
          }

        case _ ⇒
          // We may not do nothing. The buffer needs to be flushed
          // so that we do not output the evil character.

          if(-1 != indexStart) {
            print(chars, indexStart, i - indexStart)
            indexStart = -1
          }
      }

      i += 1
    }

    if(-1 != indexStart) {
      print(chars, indexStart, n - indexStart)
    }
  }

  private[this] def _and()(implicit writer: TextOutput) {
    writer.print('&')
  }

  private[this] def _semi()(implicit writer: TextOutput) {
    writer.print(';')
  }

  /**
   * Finds and returns the index for the first character that is not <code>\r</code> or <code>\n</code>.
   *
   * @param value The array of characters to check.
   *
   * @return The index of the first character not being <code>\r</code> or <code>\n</code>; <code>-1</code> if no such character exists.
   */
  private[this] def firstCharNotCRLF(value: Array[Char]): Int = {
    val n = value.length
    var i = 0

    while(i < n) {
      val char = value(i)

      if(char != '\n' && char != '\r') {
        return i
      }

      i += 1
    }

    -1
  }

  /**
   * Finds and returns the index for the last character that is not <code>\r</code> or <code>\n</code>.
   *
   * @param value The array of characters to check.
   *
   * @return The index of the last character not being <code>\r</code> or <code>\n</code>; <code>-1</code> if no such character exists.
   */
  private[this] def lastCharNotCRLF(value: Array[Char]): Int = {
    var i = value.length - 1

    while(i > -1) {
      val char = value(i)

      if(char != '\n' && char != '\r') {
        return i
      }

      i -= 1
    }

    -1
  }



  /**
   * trimHtmlText collapses consecutive white-space characters into a single white-space
   * character. This is a fast and easy approach to trim text without altering its behaviour.
   *
   * @param value The text to trim.
   * @param afterOpen Whether or not the text occurs immediately after an opening tag.
   * @param beforeClose Whether or not the text occurs immediately before a closing tag.
   *
   * @return The trimmed value; may still start or end with a whitespace character.
   */
  private[this] def trimHtmlText(value: String, afterOpen: Boolean, beforeClose: Boolean): String =
    if(null != value) {
      val length = value.length

      if(length == 1) {
        //
        // Make no attempt to trim a single character because it could be \n, \32 or anything
        // else and we do want to preserve single whitespace.
        // Unless it is \r or \n and we are right after open or before close.
        //

        if(afterOpen || beforeClose) {
          value.charAt(0) match {
            case '\r' | '\n' ⇒ ""
            case _ ⇒ value
          }
        } else {
          value
        }
      } else {
        val chars = value.toCharArray
        val fromIndex = if(afterOpen) firstCharNotCRLF(chars) else 0
        val toIndex = if(beforeClose && fromIndex >= 0) lastCharNotCRLF(chars) else (length - 1)

        if(fromIndex >= 0 && toIndex >= 0) {
          val newLength = toIndex - fromIndex + 1
          val newValue =
            if(newLength == length) {
              value
            } else {
              new String(chars, fromIndex, newLength)
            }

          val firstChar = chars(fromIndex)
          val lastChar = chars(toIndex)

          newValue match {
            case startsAndEndsWithWhitespace if firstChar <= ' ' && lastChar <= ' ' ⇒
              firstChar+startsAndEndsWithWhitespace.trim+lastChar

            case startsWithWhitespace if firstChar <= ' ' ⇒
              firstChar+startsWithWhitespace.trim

            case endsWithWhitespace if lastChar <= ' ' ⇒
              endsWithWhitespace.trim+lastChar

            case ordinaryString ⇒
              ordinaryString
          }
        } else {
          // String consists only of \r or \n characters and it was before an opening or closing tag
          // so we are allowed to return an empty string.
          ""
        }
      }
    } else if(null == value) {
      // Catch any null values and return "" instead.
      ""
    }  else {
      // Empty string.
      value
    }

  // The isName method is not used correct since prefix:label makes a name and Elem
  // does not define it as such. For now we do it like isName(prefix):isName(label)
  // so in fact the "label" part of Elem is also checked for isNameStart but in the end
  // it is only making sure that a name is correct even tough label is null.

  private[this] def isName(value: String): Boolean =
    if(null == value || value.length < 1) {
       false
     } else {
       val charArray = value.toCharArray

       if(isNameStart(charArray(0))) {
         val n = value.length
         var i = 1
         var valid = true

         while(valid && i < n) {
           valid = isNamePart(charArray(i))
           i += 1
         }

         valid
       } else {
         false
       }
     }

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
      case _ ⇒ false
      //case Extender ⇒ true
      //case Combiner ⇒ true
    }

  private[this] val ValidUnicodeEntityRegex = """^#\d{1,4}$""".r

  private[this] val ValidHashEntityRegex = """^#x[abcdefABCDEF0-9]{1,4}$""".r

  private[this] val ValidEntities =
    ImmutableSortedSet.of[String](
      "quot",  "amp", "apos", "lt", "gt", "nbsp",
      "iexcl", "cent", "pound", "curren", "yen", "brvbar",
      "sect", "uml", "copy", "ordf", "laquo", "not", "shy", "reg",
      "macr", "deg", "plusmn", "sup2", "sup3", "acute", "micro", "para",
      "middot", "cedil", "sup1", "ordm", "raquo", "frac14", "frac12", "frac34",
      "iquest", "Agrave", "Aacute", "Acirc", "Atilde", "Auml", "Aring", "AElig",
      "Ccedil", "Egrave", "Eacute", "Ecirc", "Euml", "Igrave", "Iacute", "Icirc",
      "Iuml", "ETH", "Ntilde", "Ograve", "Oacute", "Ocirc", "Otilde", "Ouml",
      "times", "Oslash", "Ugrave", "Uacute", "Ucirc", "Uuml", "Yacute", "THORN",
      "szlig", "agrave", "aacute", "acirc", "atilde", "auml", "aring", "aelig",
      "ccedil", "egrave", "eacute", "ecirc", "euml", "igrave", "iacute", "icirc",
      "iuml", "eth", "ntilde", "ograve", "oacute", "ocirc", "otilde", "ouml", "divide",
      "oslash", "ugrave", "uacute", "ucirc", "uuml", "yacute", "thorn", "yuml", "OElig",
      "oelig", "Scaron", "scaron", "Yuml", "fnof", "circ", "tilde", "Alpha", "Beta", "Gamma",
      "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu",
      "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega",
      "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota",
      "kappa", "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "sigmaf", "sigma",
      "tau", "upsilon", "phi", "chi", "psi", "omega", "thetasym", "upsih", "piv", "ensp",
      "emsp", "thinsp", "zwnj", "zwj", "lrm", "rlm", "ndash", "mdash", "lsquo", "rsquo",
      "sbquo", "ldquo", "rdquo", "bdquo", "dagger", "Dagger", "bull", "hellip", "permil", "prime",
      "Prime", "lsaquo", "rsaquo", "oline", "frasl", "euro", "image", "weierp", "real", "trade",
      "alefsym", "larr", "uarr", "rarr", "darr", "harr", "crarr", "lArr", "uArr", "rArr", "dArr",
      "hArr", "forall", "part", "exist", "empty", "nabla", "isin", "notin", "ni", "prod", "sum",
      "minus", "lowast", "radic", "prop", "infin", "ang", "and", "or", "cap", "cup", "int",
      "there4", "sim", "cong", "asymp", "ne", "equiv", "le", "ge", "sub", "sup", "nsub", "sube",
      "supe", "oplus", "otimes", "perp", "sdot", "lceil", "rceil", "lfloor", "rfloor", "lang",
      "rang", "loz", "spades", "clubs", "hearts", "diams")

  private[this] def isEntity(name: String): Boolean = {
    val firstChar = name.charAt(0)

    firstChar match {
      case '#' ⇒
        // &#DDDD; and &#xDDDD;
        // Note that name is only "#DDDD" or "#xDDDD"

        if(name.length > 6) {
          false
        } else {
          ValidUnicodeEntityRegex.pattern.matcher(name).matches() ||
          ValidHashEntityRegex.pattern.matcher(name).matches()
        }

      case _ ⇒ ValidEntities contains name
    }
  }
}
