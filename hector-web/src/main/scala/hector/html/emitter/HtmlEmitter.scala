package hector.html.emitter

import com.google.common.collect.ImmutableSortedSet

import hector.html._
import hector.util.{TextOutput, trimToOption}

import javax.annotation.Nullable
import javax.annotation.concurrent.ThreadSafe
import java.io.{PrintWriter, StringWriter}

import scala.xml.{NamespaceBinding, MetaData, Node}

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
    val stringBuilder = new StringBuilder()
    val writer = new TextOutput(stringBuilder, humanReadable)

    if(!omitDocType) {
      _dtd(docType)(writer)
    }

    visit(html, docType, stripComments, trim, humanReadable)(writer)

    stringBuilder.toString()
  }

  private[this] def visit(node: Node, docType: DocType, stripComments: Boolean, trim: Boolean, humanReadable: Boolean)(implicit writer: TextOutput) {
    import scala.xml._

    // Subsequent whitespace could be removed. This should be something we have to consider since
    // it will generate an Html output that is much easier on the eye when looking at the source.

    node match {
      case Text(value) ⇒
        _string(value, trim)

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
          _tag(prefix, label, attributes, scope, docType, stripComments, trim, humanReadable)
          if(docType != DocTypes.`HTML 5` && docType != DocTypes.`XHTML 5`) {
            _tagCloseShort()
          } else {
            _gt()
          }
        } else {
          _lt()
          _tag(prefix, label, attributes, scope, docType, stripComments, trim, humanReadable)
          _gt()

          _newLineOpt()
          writer.pushIndent()

          val iterator = children.iterator

          while(iterator.hasNext) {
            visit(iterator.next(), docType, stripComments, trim, humanReadable)
          }

          _tagCloseLong(prefix, label)
        }
        _newLineOpt()

      case Group(nodes) ⇒
        val iterator = nodes.iterator

        while(iterator.hasNext) {
          visit(iterator.next(), docType, stripComments, trim, humanReadable)
        }

      case Unparsed(data) ⇒
        writer.print(data)

      case EntityRef(name) ⇒
        _entity(name)

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

      case Comment(text) ⇒
        _commentOpen()
        _string(text, trim)
        _commentClose()
        _newLineOpt()

      case atom: Atom[_] ⇒
        // Apparently someone decided to name PCDATA not PCDATA but Atom.
        // So we will have to treat it like parsed character data.
        _string(atom.data.toString, trim)
    }
  }

  @inline
  private[this] def _dtd(docType: DocType)(implicit writer: TextOutput) {
    writer.print(docType.charArray)
    _newLine()
  }

  private[this] def _tag(@Nullable prefix: String, label: String, @Nullable attributes: MetaData, scope: NamespaceBinding, docType: DocType, stripComments: Boolean, trim: Boolean, humanReadable: Boolean)(implicit writer: TextOutput) {
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
      // Apparently calling hasNext would lead to an error and instead we must check
      // for Null.
      //

      // We tested for Null in the header.

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

        // if(value forall { _.isInstanceOf[Text] }) {
        //
        // The common case: All attribute elements are text nodes. Who would have
        // thought about that?!
        //
        // This is an evil side-effect we are willing to introduce into our beautiful
        // map operation so we do not have to call (stringSequence map { _.length }).sum
        // which makes us traverse the sequence again.
        //
        // Please note that we are going for an imperative style here since otherwise
        // the totalLength will lead to an IntRef which is instantiated for each
        // attribute with an anonymous function for the map operation.
        //
        // The old code which used to do this:
        //
        // var totalLength = 0
        //
        // val stringSequence: Seq[String] = value map {
        //   textNode ⇒
        //     val result = textNode.asInstanceOf[Text].data
        //     totalLength += result.length
        //     result
        // }
        //
        // // Use "totalLength" instead of "(stringSequence map { _.length }).sum" here.
        // val stringBuilder = new StringBuilder(totalLength)
        // val iterator = stringSequence.iterator
        //
        // while(iterator.hasNext) {
        //  stringBuilder.append(iterator.next())
        // }
        //

        val iterator = value.iterator

        while(iterator.hasNext) {
          //FIXME(joa): I think it is possible to have Unparsed here too.

          // Never trim any attributes because this is absolutely the decision of the
          // person generating an attribute in the first place.

          _string(iterator.next().asInstanceOf[Text].data, trim = false)
        }

        writer.print('"')
      }
    }

    //TODO(joa): how did he implement namespaces?! its quite weird ...
    //scope
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

  @inline
  private[this] def _tagCloseShort()(implicit writer: TextOutput) {
    writer.print('/')
    _gt()
  }

  @inline
  private[this] def _entity(name: String)(implicit writer: TextOutput) {
    if(isEntity(name)) {
      _and()
      writer.print(name)
      _semi()
    }
  }

  @inline
  private[this] def _prefix(prefix: String)(implicit writer: TextOutput) {
    if(isName(prefix)) {
      writer.print(prefix)
    } else {
      writer.print("invalidName")
    }
  }

  @inline
  private[this] def _name(name: String)(implicit writer: TextOutput) {
    if(isName(name)) {
      writer.print(name)
    } else {
      writer.print("invalidName")
    }
  }

  @inline
  private[this] def _cdataOpen()(implicit writer: TextOutput) {
    writer.print(CharsCDataOpen)
  }

  @inline
  private[this] def _cdataClose()(implicit writer: TextOutput) {
    writer.print(CharsCDataClose)
    writer.newLineOpt()
  }

  @inline
  private[this] def _procInstrOpen()(implicit writer: TextOutput) {
    writer.print(CharsProcInstrOpen)
  }

  @inline
  private[this] def _procInstrClose()(implicit writer: TextOutput) {
    writer.print(CharsProcInstrClose)
    writer.newLineOpt()
  }

  @inline
  private[this] def _colon()(implicit writer: TextOutput) {
    writer.print(':')
  }

  @inline
  private[this] def _lt()(implicit writer: TextOutput) {
    writer.print('<')
  }

  @inline
  private[this] def _gt()(implicit writer: TextOutput) {
    writer.print('>')
  }

  @inline
  private[this] def _commentOpen()(implicit writer: TextOutput) {
    writer.print(CharsCommentOpen)
    _spaceOpt()
  }

  @inline
  private[this] def _commentClose()(implicit writer: TextOutput) {
    _spaceOpt()
    writer.print(CharsCommentClose)
  }

  @inline
  private[this] def _newLine()(implicit writer: TextOutput) {
    writer.newLine()
  }

  @inline
  private[this] def _newLineOpt()(implicit writer: TextOutput) {
    writer.newLineOpt()
  }

  @inline
  private[this] def _space()(implicit writer: TextOutput) {
    writer.print(' ')
  }

  @inline
  private[this] def _spaceOpt()(implicit writer: TextOutput) {
    writer.printOpt(' ')
  }

  @inline
  private[this] def _string(value: String, trim: Boolean)(implicit writer: TextOutput) {
    //TODO(joa): use proper escape method ...
    writer.print(xml.Utility.escape(if(trim) trimHtmlText(value) else value))
  }

  @inline
  private[this] def _and()(implicit writer: TextOutput) {
    writer.print('&')
  }

  @inline
  private[this] def _semi()(implicit writer: TextOutput) {
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
  private[this] def trimHtmlText(value: String): String =
    if(null != value) {
      val length = value.length

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

  // The isName method is not used correct since prefix:label makes a name and Elem
  // does not define it as such. For now we do it like isName(prefix):isName(label)
  // so in fact the "label" part of Elem is also checked for isNameStart but in the end
  // it is only making sure that a name is correct even tough label is null.

  @inline
  private[this] def isName(value: String): Boolean =
    testStringForValidity(value, isNameStart, isNamePart)

  /**
   * http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
   * @param char
   * @return
   */
  @inline
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
  @inline
  private[this] def isNamePart(char: Char): Boolean =
    char match {
      case letter if Character.isLetter(letter) ⇒ true
      case digit if Character.isDigit(digit) ⇒ true
      case '.' | '-' | '_' | ':' ⇒ true
      //case Extender ⇒ true
      //case Combiner ⇒ true
    }

  private[this] def testStringForValidity(value: String, startIsValid: Char ⇒ Boolean, partIsValid: Char ⇒ Boolean): Boolean = {
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

  @inline
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
