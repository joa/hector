package hector.js.emitter

import javax.annotation.concurrent.NotThreadSafe
import java.io.PrintWriter

/**
 */
@NotThreadSafe
private[emitter] final class JsWriter(private[this] val writer: PrintWriter, private[this] val humanReadable: Boolean) {
  private[this] var indentLevel = 0

  private[this] var indents = Array(Array[Char](0))

  private[this] var wasNewline = false

  def pushIndent() {

    indentLevel += 1

    if(indentLevel >= indents.length) {
      val newIndentLevel = Array.fill(indentLevel << 1) {
        ' '
      }
      val newIndents = new Array[Array[Char]](indents.length + 1)

      System.arraycopy(indents, 0, newIndents, 0, indents.length)

      newIndents(indentLevel) = newIndentLevel
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
