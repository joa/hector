package hector.html.emitter

import hector.HectorSpec
import scala.xml._

/**
 */
final class HtmlEmitterSpec extends HectorSpec {
  private[this] val DocType = "<!DOCTYPE html>\n"

  describe("HtmlEmitter.toString") {
    it("should collapse whitespace into a single whitespace character when trim = true and humanReadable = false") {
      val text0 = "   foo bar   "
      val output0 = HtmlEmitter.toString(<a>{text0}{text0}</a>, trim = true, humanReadable = false)

      output0 must be (DocType+"<a> foo bar  foo bar </a>")

      val text1 = "\n   foo bar   \n"
      val output1 = HtmlEmitter.toString(<a>{text1}{text1}</a>, trim = true, humanReadable = false)

      output1 must be (DocType+"<a> foo bar\n\nfoo bar </a>")

      val text2 = "\nfoo bar\n"
      val output2 = HtmlEmitter.toString(<a>{text2}</a>, trim = true, humanReadable = false)

      output2 must be (DocType+"<a>foo bar</a>")
    }

    it("should not trim text inside a <pre>-context when humanReadable = false") {
      val text = "   foo bar   "
      val output = HtmlEmitter.toString(<pre>{text}</pre>, trim = true)

      output must be (DocType+"<pre>"+text+"</pre>")
    }

    it("should not trim text inside a <pre>-context when humanReadable = true") {
      val text = "   foo bar   "
      val output = HtmlEmitter.toString(<pre>{text}</pre>, trim = true, humanReadable = true)

      output must be (DocType+"<pre>"+text+"</pre>")
    }

    it("should not trim text inside attribute values") {
      val text = "   foo   bar   "
      val output = HtmlEmitter.toString(<span attr={text}>baz</span>, trim = true)

      output must be (DocType+"<span attr=\""+text+"\">baz</span>")
    }

    // Check for valid characters in output

    it("should ignore invalid characters") {
      val output = HtmlEmitter.toString(Text("foo\01bar"))

      output must be (DocType+"foobar")
    }

    // Check for valid names and entities

    it("should replace names that start with an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem(null, " notvalid", Null, TopScope))

      output must be (DocType+"<invalidName>")
    }

    it("should replace names that contain an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem(null, "not valid", Null, TopScope))

      output must be (DocType+"<invalidName>")
    }

    it("should replace prefixes that start with an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem(" notvalid", "valid", Null, TopScope))

      output must be (DocType+"<invalidName:valid>")
    }

    it("should replace prefixes that contain an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem("not valid", "valid", Null, TopScope))

      output must be (DocType+"<invalidName:valid>")
    }

    it("should replace attribute names that start with an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem("valid", "valid", Attribute(" notvalid", Text("valid"), Null), TopScope))

      output must be (DocType+"<valid:valid invalidName=\"valid\">")
    }

    it("should replace attribute names that contain an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem("valid", "valid", Attribute("not valid", Text("valid"), Null), TopScope))

      output must be (DocType+"<valid:valid invalidName=\"valid\">")
    }

    it("should replace attribute prefixes that start with an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem("valid", "valid", Attribute(" notvalid", "valid", Text("valid"), Null), TopScope))

      output must be (DocType+"<valid:valid invalidName:valid=\"valid\">")
    }

    it("should replace attribute prefixes that contain an invalid character with \"invalidName\"") {
      val output = HtmlEmitter.toString(Elem("valid", "valid", Attribute("not valid", "valid", Text("valid"), Null), TopScope))

      output must be (DocType+"<valid:valid invalidName:valid=\"valid\">")
    }

    it("should not ignore valid entities") {
      HtmlEmitter.toString(EntityRef("amp")) must be (DocType+"&amp;")

      HtmlEmitter.toString(EntityRef("#0")) must be (DocType+"&#0;")
      HtmlEmitter.toString(EntityRef("#01")) must be (DocType+"&#01;")
      HtmlEmitter.toString(EntityRef("#012")) must be (DocType+"&#012;")
      HtmlEmitter.toString(EntityRef("#0123")) must be (DocType+"&#0123;")

      HtmlEmitter.toString(EntityRef("#xa")) must be (DocType+"&#xa;")
      HtmlEmitter.toString(EntityRef("#xaf")) must be (DocType+"&#xaf;")
      HtmlEmitter.toString(EntityRef("#xaff")) must be (DocType+"&#xaff;")
      HtmlEmitter.toString(EntityRef("#xaffe")) must be (DocType+"&#xaffe;")

      HtmlEmitter.toString(EntityRef("#xA")) must be (DocType+"&#xA;")
      HtmlEmitter.toString(EntityRef("#xAF")) must be (DocType+"&#xAF;")
      HtmlEmitter.toString(EntityRef("#xAFF")) must be (DocType+"&#xAFF;")
      HtmlEmitter.toString(EntityRef("#xAFFE")) must be (DocType+"&#xAFFE;")
    }

    it("should ignore invalid entities") {
      HtmlEmitter.toString(EntityRef("does not exist")) must be (DocType)
      HtmlEmitter.toString(EntityRef("#01234")) must be (DocType)
      HtmlEmitter.toString(EntityRef("#-1")) must be (DocType)

      HtmlEmitter.toString(EntityRef("#x01234")) must be (DocType)
      HtmlEmitter.toString(EntityRef("#xg")) must be (DocType)
      HtmlEmitter.toString(EntityRef("#xG")) must be (DocType)
    }
  }
}
