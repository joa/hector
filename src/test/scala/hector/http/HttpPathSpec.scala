package hector.http

import hector.HectorSpec

/**
 * @author Joa Ebert
 */
final class HttpPathSpec extends HectorSpec {
  describe("HttpPath.fromString") {
    it("should parse a URI without a trailing slash") {
      val uri = "/foo/bar"

      HttpPath.fromString(uri) match {
        case 'foo /: 'bar /: No_/ =>
        case other => fail("Did not expect "+other)
      }
    }

    it("should parse a URI with a trailing slash") {
      val uri = "/foo/bar/"

      HttpPath.fromString(uri) match {
        case 'foo /: 'bar /: Required_/ =>
        case other => fail("Did not expect "+other)
      }
    }

    it("should parse \"/\" as Required_/") {
      val uri = "/"

      HttpPath.fromString(uri) match {
        case Required_/ =>
        case other => fail("Did not expect "+other)
      }
    }

    it("should parse \"\" as No_/") {
      HttpPath.fromString("") match {
        case No_/ =>
        case other => fail("Did not expect "+other)
      }
    }

    it("should omit empty parts in a URI") {
      val uri = "/foo//bar/baz"

      HttpPath.fromString(uri) match {
        case 'foo /: 'bar /: 'baz /: _ =>
        case other => fail("Did not expect "+other)
      }
    }

    it("should support filenames") {
      val uri = "/foo//bar.baz"

      HttpPath.fromString(uri) match {
        case 'foo /: Symbol("bar.baz") /: No_/ =>
        case other => fail("Did not expect "+other)
      }
    }
  }
}
