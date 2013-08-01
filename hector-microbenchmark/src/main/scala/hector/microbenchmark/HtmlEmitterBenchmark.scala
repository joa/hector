package hector.microbenchmark

import com.google.caliper.{Runner, Param}
import com.google.common.io.Resources

import hector.html.emitter.HtmlEmitter
import hector.html._
import hector.microbenchmark.util.HectorBenchmark

import scala.xml._

/**
 */
object HtmlEmitterBenchmark {
  def main(args: Array[String]) {
    Runner.main(classOf[HtmlEmitterBenchmark], args)
  }
}

/**
 */
final class HtmlEmitterBenchmark extends HectorBenchmark {
  @Param(Array("0", "1", "2", "3", "4"))
  private var index: Int = _

  private var data: Node = _

  override protected def setUp() {
    data = Data(index)
  }

  def timeHtmlEmitterWithTrimAndStripComments(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = true, trim = true, humanReadable = false)
    }

  def timeHtmlEmitterWithTrim(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = false, trim = true, humanReadable = false)
    }

  def timeHtmlEmitterWithStripComments(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = true, trim = false, humanReadable = false)
    }

  def timeHtmlEmitter(reps: Int) =
    benchmark(reps) {
      HtmlEmitter.toString(data, DocTypes.`HTML 5`, stripComments = false, trim = false, humanReadable = false)
    }

  def timeToString(reps: Int) =
    benchmark(reps) {
      data.toString()
    }

  def timeUtilityToXMLWithTrimAndStripComments(reps: Int) =
    benchmark(reps) {
      Utility.toXML(Utility.trim(data), stripComments = true, preserveWhitespace = false).toString()
    }

  def timeUtilityToXMLWithTrim(reps: Int) =
    benchmark(reps) {
      Utility.toXML(Utility.trim(data), preserveWhitespace = false).toString()
    }

  def timeUtilityToXMLWithStripComments(reps: Int) =
    benchmark(reps) {
      Utility.toXML(data, stripComments = true).toString()
    }

  def timeUtilityToXML(reps: Int) =
    benchmark(reps) {
      Utility.toXML(data).toString()
    }

  def timeUtilitySerializeWithTrimAndStripComments(reps: Int) =
    benchmark(reps) {
      Utility.serialize(Utility.trim(data), stripComments = true, preserveWhitespace = false).toString()
    }

  def timeUtilitySerializeWithTrim(reps: Int) =
    benchmark(reps) {
      Utility.serialize(Utility.trim(data), preserveWhitespace = false).toString()
    }

  def timeUtilitySerializeWithStripComments(reps: Int) =
    benchmark(reps) {
      Utility.serialize(data, stripComments = true).toString()
    }

  def timeUtilitySerialize(reps: Int) =
    benchmark(reps) {
      Utility.serialize(data).toString()
    }
}

object Data {
  def apply(index: Int) =
    index match {
      case 0 ⇒ Books
      case 1 ⇒ YQL_1
      case 2 ⇒ W3_frontpage
      case 3 ⇒ Generatedata_com
      case 4 ⇒ W3_html5spec
    }

  private[this] val Books  = load("books")

  private[this] val YQL_1 = load("yql_1")

  private[this] val W3_frontpage = load("w3_frontpage")

  private[this] val Generatedata_com = load("generatedata_com")

  private[this] val W3_html5spec = load("w3_html5spec")

  private[this] def load(file: String) =
    XML.load(Resources.getResource(file+".xml"))
}
