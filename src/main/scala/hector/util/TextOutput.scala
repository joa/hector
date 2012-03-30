package hector.util

import java.io.PrintWriter

import javax.annotation.concurrent.NotThreadSafe

/**
 * The TextOutput class wraps a PrintWriter to generate human-readable textual output.
 *
 * <p>If <code>humanReadable</code> is <code>true</code> all methods suffixed with <code>Opt</code>
 * will perform their corresponding action.</p>
 *
 * <p>Indentation is generated only when the output should be human-readable.</p>
 */
@NotThreadSafe
final class TextOutput(
  /** The actual PrintWriter. */
  private[this] val writer: PrintWriter,
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
    writer.print('\n')
    wasNewline = true
  }

  def newLineOpt() {
    if(humanReadable) {
      newLine()
    }
  }

  def print(value: Char) {
    maybeIndent()
    writer.print(value)
    wasNewline = false
  }

  def print(value: Array[Char]) {
    maybeIndent()
    writer.print(value)
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
      writer.print(indents(indentLevel))
      wasNewline = false
    }
  }
}

