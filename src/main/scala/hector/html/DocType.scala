package hector.html

sealed abstract class DocType(val charArray: Array[Char]) {
  override def toString = String.valueOf(charArray)
}

/**
 * The DocType object is an enumeration of DTDs.
 *
 * @see http://www.w3schools.com/tags/tag_doctype.asp
 * @see http://en.wikipedia.org/wiki/Document_Type_Declaration
 */
object DocTypes {
  /** The case-insensitive HTML 5 DOCTYPE. */
  object `HTML 5` extends DocType("<!DOCTYPE html>".toCharArray)

  /** The case-sensitive XHTML5 DOCTYPE. XHTML5 content must be served with no other MIME type than <code>application/xhtml+xml</code>. */
  object `XHTML 5` extends DocType("<!DOCTYPE html>".toCharArray)

  /** This DTD contains all HTML elements and attributes, but does NOT INCLUDE presentational or deprecated elements (like font). Framesets are not allowed. */
  object `HTML 4.01 Strict` extends DocType("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">".toCharArray)

  /** This DTD contains all HTML elements and attributes, INCLUDING presentational and deprecated elements (like font). Framesets are not allowed. */
  object `HTML 4.01 Transitional` extends DocType("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">".toCharArray)

  /** This DTD is equal to HTML 4.01 Transitional, but allows the use of frameset content. */
  object `HTML 4.01 Frameset` extends DocType("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">".toCharArray)

  /** This DTD contains all HTML elements and attributes, but does NOT INCLUDE presentational or deprecated elements (like font). Framesets are not allowed. The markup must also be written as well-formed XML. */
  object `XHTML 1.0 Strict` extends DocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">".toCharArray)

  /** This DTD contains all HTML elements and attributes, INCLUDING presentational and deprecated elements (like font). Framesets are not allowed. The markup must also be written as well-formed XML. */
  object `XHTML 1.0 Transitional` extends DocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">".toCharArray)

  /** This DTD is equal to XHTML 1.0 Transitional, but allows the use of frameset content. */
  object `XHTML 1.0 Frameset` extends DocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">".toCharArray)

  /** This DTD is equal to XHTML 1.0 Strict, but allows you to add modules (for example to provide ruby support for East-Asian languages). */
  object `XHTML 1.1` extends DocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">".toCharArray)
}
