package hector.util

import javax.annotation.concurrent.NotThreadSafe

/**
 * The TextOutput class wraps a StringBuilder to generate human-readable textual output.
 *
 * <p>If <code>humanReadable</code> is <code>true</code> all methods suffixed with <code>Opt</code>
 * will perform their corresponding action.</p>
 *
 * <p>Indentation is generated only when the output should be human-readable.</p>
 */
@NotThreadSafe
final class TextOutput(
  /** The actual builder. */
  private[this] val builder: StringBuilder,

  /** Whether or not human readable text should be generated. */
  private[this] val humanReadable: Boolean) {

  /** The indentation level. */
  private[this] var indentLevel = 0

  /** The cached identation levels. */
  private[this] var indents = Array(Array[Char](0))

  /** Whether or not newLine() or newLineOpt() were called. */
  private[this] var wasNewline = false

  def pushIndent() {
    indentLevel += 1

    if(indentLevel >= indents.length) {
      val newIndentLevel = Array.fill(indentLevel << 1) { ' ' }
      val newIndents = new Array[Array[Char]](indents.length + 1)

      System.arraycopy(indents, 0, newIndents, 0, indents.length)

      newIndents(indentLevel) = newIndentLevel
      indents = newIndents
    }
  }

  def popIndent() {
    indentLevel -= 1
  }

  def newLine() {
    builder.append('\n')
    wasNewline = true
  }

  def newLineOpt() {
    if(humanReadable) {
      newLine()
    }
  }

  def print(value: Char) {
    maybeIndent()
    builder.append(value)
    wasNewline = false
  }

  def print(value: Array[Char]) {
    maybeIndent()
    builder.appendAll(value)
    wasNewline = false
  }

  def print(value: Array[Char], offset: Int, length: Int) {
    maybeIndent()
    builder.appendAll(value, offset, length)
    wasNewline = false
  }

  def print(value: String) {
    print(value.toCharArray)
  }

  def printOpt(value: Char) {
    if(humanReadable) {
      print(value)
    }
  }

  def printOpt(value: Array[Char]) {
    if(humanReadable) {
      print(value)
    }
  }

  def printOpt(value: String) {
    if(humanReadable) {
      print(value.toCharArray)
    }
  }

  private[this] def maybeIndent() {
    if(wasNewline && humanReadable) {
      if(indentLevel > 0) {
        //
        // We perform the check for indentLevel > 0 because Java does something
        // weird when emitting an empty array of characters and it makes testing
        // much harder because "a b" is no longer equal to "a b". Go figure.
        //

        builder.appendAll(indents(indentLevel))
      }

      wasNewline = false
    }
  }
}

