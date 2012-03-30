package hector

import java.util.UUID

package object util {
  // Utility methods should perform as fast as possible. Therefore you will not find many use
  // of Scala features like for-comprehension in here.
  //

  val HexDigits: Array[Char] =
    Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  /**
   * Converts an array of bytes into a string.
   *
   * For instance <code>Array(0xff, 0xa0, 0x08, 0x00)</code> would result in the string "ffa00800".
   *
   * @param hash The array of bytes to convert.
   *
   * @return The converted string with. The length is exactly twice the one of the given array.
   */
  def convertBytesToHexString(hash: Array[Byte]): String =
    convertBytesToHexString(new StringBuilder(hash.length << 1), hash)

  /**
   * Appends a given array of bytes to a StringBuilder by converting and returns the resulting string.
   *
   * @param stringBuilder StringBuilder to append to.
   * @param hash The array of bytes to append.
   *
   * @return The resulting string.
   */
  def convertBytesToHexString(stringBuilder: StringBuilder, hash: Array[Byte]): String = {
    var i = 0
    val n = hash.length

    while(i < n) {
      val value = hash(i) & 0xff

      if(value < 0x10) {
        stringBuilder append '0'
      }

      stringBuilder append Integer.toString(value, 16)

      i += 1
    }

    stringBuilder.toString()
  }

  private[this] val LetItCrashEnabled = System.getProperty("hector.enableLIC", "false").toBoolean

  /**
   * Creates and throws a RuntimeException with the message "Let it Crash!" for a given probability.
   *
   * <p>In order to enable this feature the JVM must be started with the parameter <code>-Dhector.enableLIC=true</code>.</p>
   *
   * <p>This is disabled by default.</p>
   *
   * <p>An exception is throw if <code>scala.math.random</code> returns a value less than the
   * given probability.</p>
   *
   * @param probability The probability value in [0, 1).
   */
  def letItCrash(probability: Double = 0.06125) {
    if(LetItCrashEnabled && math.random < probability) {
      println("Simulated exception.")
      throw new RuntimeException("Let it Crash!")
    }
  }

  /**
   * Trims a string and returns None in case the result is empty.
   *
   * <p>In case <code>null</code> is passed to this method <code>None</code> will be returned.</p>
   *
   * {{{
   *   trimToOption(null) == None
   *   trimToOption("") == None
   *   trimToOption(" ") == None
   *   trimToOption(" foo ") == Some("foo")
   *   trimToOption("bar") == Some("bar")
   * }}}
   *
   * @param value The string to trim.
   *
   * @return <code>Some</code> value if not empty after trimming; <code>None</code> otherwise.
   */
  def trimToOption(value: String): Option[String] =
    if(null == value || value.length == 0) {
      None
    } else {
      val trimmedString = value.trim

      if(trimmedString.length == 0) {
        None
      } else {
        Some(trimmedString)
      }
    }

  /**
   * Generates and returns JavaScript code which will evaluate to the given value.
   *
   * @param value The string to escape.
   *
   * @return JavaScript code which will evaluate to the given string.
   */
  def escapeJavaScriptString(value: String): String = {
    // This code is a shameless adoption of GWT's awesome javaScriptString function in
    // their JsToStringGenerationVisitor which is itself an adoption of Rhino's escapeString.

    val chars = value.toCharArray
    val n = chars.length

    var i = 0

    // Count the number of quotes and apostrophes. Use the least used so less characters need to
    // be escaped.
    //
    // We could use quoteCount = chars count { _ == '"' } but in that case we would have to
    // traverse the array twice. One time for quotes, one time for apostrophes.

    var quoteCount = 0
    var aposCount = 0

    while(i < n) {
      chars(i) match {
        case '"' ⇒ quoteCount += 1
        case '\'' ⇒ aposCount += 1
        case _ ⇒
      }

      i += 1
    }

    val result = new StringBuilder(value.length + 0x10)
    val quoteChar = if(quoteCount < aposCount) '"' else '\''

    // Append opening quote character.

    result.append(quoteChar)

    // Append all characters

    i = 0

    while(i < n) {
      val char = chars(i)

      if(' ' <= char && char <= '~' && char != quoteChar && char != '\\') {
        // An ordinary print character (like C isprint())
        result.append(char)
      } else {
        // Character needs to be escaped. It is either a standard escape character like \n or it
        // might be a unicode character. If it is not a standard escape character we will use
        // either octal, hexadecimal or unicode encoding depending on which variant is shorter.
        //
        // Note that octal encoding might be used only if we are at the end of the string or
        // when the subsequent character is not a number since it would be misinterpreted otherwise.

        val escape: Int =
          char match {
            case '\b' ⇒ 'b'
            case '\f' ⇒ 'f'
            case '\n' ⇒ 'n'
            case '\r' ⇒ 'r'
            case '\t' ⇒ 'r'
            case '"' ⇒ '"'   // Will be reached only if c == quoteChar
            case '\'' ⇒ '\'' // Will be reached only if c == quoteChar
            case '\\' ⇒ '\\'
            case _ ⇒ -1
          }

        result.append('\\')

        if(escape >= 0) {
          // An \escape sort of character
          result.append(escape.asInstanceOf[Char])
        } else {
          if(char < ' ' && (i == (n-1) || chars(i + 1) < '0' || chars(i + 1) > '9')) {
            if(char > 0x7) {
              result.append(('0' + (0x7 & (char >> 3))).asInstanceOf[Char])
            }

            result.append(('0' + (0x7 & char)).asInstanceOf[Char])
          } else {
            val hexSize =
              if(char < 0x100) {
                result.append('x')
                2
              } else {
                result.append('u')
                4
              }

            var shift = (hexSize - 1) << 2

            while(shift >= 0) {
              val digit = 0xf & (char >> shift)
              result.append(HexDigits(digit))
              shift -= 4
            }
          }
        }
      }

      i += 1
    }

    // Append closing quote character.

    result.append(quoteChar)

    // Escape all closing XML tags

    escapeClosingTags(result)

    result.toString()
  }

  /**
   * Helper method for <code>escapeJavaScriptString</code>.
   *
   * Escapes any closing XML tags embedded in the given StringBuilder which could
   * potentially cause a parser failure in a browser.
   *
   * @param stringBuilder The string builder. May be <code>null</code>.
   */
  private[this] def escapeClosingTags(stringBuilder: StringBuilder) {
    if(null != stringBuilder) {
      var index = 0

      //TODO(joa): check bytecode
      while({index = stringBuilder.indexOf("</"); index} != -1) {
        stringBuilder.insert(index + 1, '\\')
      }
    }
  }

  def randomHash(): String = {
    import java.util.{UUID ⇒ JUUID}

    val uuid = JUUID.randomUUID().toString
    uuid.replace("-", "")
  }

}
