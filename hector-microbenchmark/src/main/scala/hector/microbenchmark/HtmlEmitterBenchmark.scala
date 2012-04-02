package hector.microbenchmark

import com.google.caliper.Param
import com.google.common.io.Resources

import hector.html.emitter.HtmlEmitter
import hector.html._
import hector.microbenchmark.util.HectorBenchmark

import scala.xml._

/**
 */
final class HtmlEmitterBenchmark extends HectorBenchmark {
  @Param(Array("0", "1", "2"))
  private var index: Int = _

  private var data: Node = _

  override protected def setUp() {
    data = Data(index)
  }

  def timeHtmlEmitterWithTrimAndStripComments(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = true, trim = true, humanReadable = false, omitDocType = false)
    }

  def timeHtmlEmitterWithTrim(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = false, trim = true, humanReadable = false, omitDocType = false)
    }

  def timeHtmlEmitterWithStripComments(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = true, trim = false, humanReadable = false, omitDocType = false)
    }

  def timeHtmlEmitter(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = false, trim = false, humanReadable = false, omitDocType = false)
    }

  def timeToString(reps: Int) =
    benchmark(reps) {
      data.toString()
    }

  def timeUtilityWithTrimAndStripComments(reps: Int) =
    benchmark(reps) {
      Utility.toXML(Utility.trim(data), stripComments = true, preserveWhitespace = false).toString()
    }

  def timeUtilityWithTrim(reps: Int) =
    benchmark(reps) {
      Utility.toXML(Utility.trim(data), preserveWhitespace = false).toString()
    }

  def timeUtilityWithStripComments(reps: Int) =
    benchmark(reps) {
      Utility.toXML(data, stripComments = true).toString()
    }

  def timeUtility(reps: Int) =
    benchmark(reps) {
      Utility.toXML(data).toString()
    }
}

object Data {
  def apply(index: Int) =
    index match {
      case 0 => Books
      case 1 => YQL_1
      case 2 => W3_frontpage
    }

  private[this] val Books  = load("books")

  private[this] val YQL_1 = load("yql_1")

  private[this] val W3_frontpage = load("w3_frontpage")

  private[this] def load(file: String) =
    XML.load(Resources.getResource(file+".xml"))
}
