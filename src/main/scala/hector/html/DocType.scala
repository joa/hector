package hector.html

/**
 * The DocType object is an enumeration of DTDs.
 *
 * @see http://www.w3schools.com/tags/tag_doctype.asp
 * @see http://en.wikipedia.org/wiki/Document_Type_Declaration
 */
object DocType {
  /** The case-insensitive HTML 5 DOCTYPE. */
  val `HTML 5` = "<!DOCTYPE html>"

  /** The case-sensitive XHTML5 DOCTYPE. XHTML5 content must be served with no other MIME type than <code>application/xhtml+xml</code>. */
  val `XHTML 5` = "<!DOCTYPE html>"

  /** This DTD contains all HTML elements and attributes, but does NOT INCLUDE presentational or deprecated elements (like font). Framesets are not allowed. */
  val `HTML 4.01 Strict` = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"

  /** This DTD contains all HTML elements and attributes, INCLUDING presentational and deprecated elements (like font). Framesets are not allowed. */
  val `HTML 4.01 Transitional` = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">"

  /** This DTD is equal to HTML 4.01 Transitional, but allows the use of frameset content. */
  val `HTML 4.01 Frameset` = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">"

  /** This DTD contains all HTML elements and attributes, but does NOT INCLUDE presentational or deprecated elements (like font). Framesets are not allowed. The markup must also be written as well-formed XML. */
  val `XHTML 1.0 Strict` = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"

  /** This DTD contains all HTML elements and attributes, INCLUDING presentational and deprecated elements (like font). Framesets are not allowed. The markup must also be written as well-formed XML. */
  val `XHTML 1.0 Transitional` = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"

  /** This DTD is equal to XHTML 1.0 Transitional, but allows the use of frameset content. */
  val `XHTML 1.0 Frameset` = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">"

  /** This DTD is equal to XHTML 1.0 Strict, but allows you to add modules (for example to provide ruby support for East-Asian languages). */
  val `XHTML 1.1` = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
}
