package hector.http

import hector.HectorSpec
import hector.http.conversion.HttpPathConversion

/**
 */
final class HttpPathSpec extends HectorSpec {
  describe("HttpPath.fromString") {
    it("should return No_/ for null") {
      HttpPathConversion.fromString(null) should be (No_/)
    }

    it("should parse a URI without a trailing slash") {
      val uri = "/foo/bar"

      HttpPathConversion.fromString(uri) match {
        case "foo" /: "bar" /: No_/ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }

    it("should parse a URI with a trailing slash") {
      val uri = "/foo/bar/"

      HttpPathConversion.fromString(uri) match {
        case "foo" /: "bar" /: Required_/ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }

    it("should parse \"/\" as Required_/") {
      val uri = "/"

      HttpPathConversion.fromString(uri) match {
        case Required_/ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }

    it("should parse \"\" as No_/") {
      HttpPathConversion.fromString("") match {
        case No_/ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }

    it("should omit empty parts in a URI") {
      val uri = "/foo//bar/baz"

      HttpPathConversion.fromString(uri) match {
        case "foo" /: "bar" /: "baz" /: _ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }

    it("should support filenames") {
      val uri = "/foo//bar.baz"

      HttpPathConversion.fromString(uri) match {
        case "foo" /: "bar.baz" /: No_/ ⇒
        case other ⇒ fail("Did not expect "+other)
      }
    }
  }
}
