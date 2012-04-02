package hector.http


/**
 * The HttpPath trait represents a request URI.
 *
 * <p>
 *   A path like "/foo/bar/baz" would be represented as
 *   <code>'foo /: 'bar /: 'baz /: No_/</code>
 *   whereas "/foo/bar/baz/" would be represented as
 *   <code>'foo /: 'bar /: 'baz /: Required_/</code>.
 * </p>
 *
 * <p>
 *   A match on such a path could look like the following example:
 *{{{  val path: HttpPath
 *
 *   path match {
 *     case 'foo /: 'bar /: 'baz /: No_/ ⇒ println("Got /foo/bar/baz")
 *     case 'foo /: 'bar /: 'baz /: Required_/ ⇒ println("Got /foo/bar/baz/")
 *     case 'f00 /: 'b4r /: 'b4z /: _ ⇒ println("Got /f00/b4r/b4z or /f00/b4r/b4z/ or /f00/b4r/b4z/[unknown]")
 *   }
 * }}}</p>
 */
sealed trait HttpPath extends Serializable {
  /**
   * Concatenates this path with the given string.
   *
   * @param that The new head.
   * @return <code>that /: this</code>
   */
  def /:(that: String) = new /:(that, this)

  /**
   * The head of the path.
   */
  def head: String

  /**
   * The tail of the path.
   */
  def tail: HttpPath

  /**
   * The head as an Option.
   * @return <code>Some</code> if the path is not empty; <code>None</code> otherwise.
   */
  def headOption: Option[String]

  /** <code>true</code> if the path is empty; <code>false</code> otherwise. */
  def isEmpty: Boolean

  /** <code>true</code> if the path is not empty; <code>false</code> otherwise. */
  def nonEmpty: Boolean
}

/**
 * The HttpPathNil trait represents the end of an HttpPath.
 */
sealed trait HttpPathNil extends HttpPath {
  override def head: String = throw new NoSuchElementException("Head of empty HttpPath.")

  override def tail: HttpPath = this

  override def headOption: Option[String] = None

  override def isEmpty: Boolean = true

  override def nonEmpty: Boolean = false
}

/**
 * The cons object of an HttpPath.
 *
 * @param head The head of the path.
 * @param tail The tail of the path.
 */
final case class /:(head: String, tail: HttpPath) extends HttpPath {
  override def headOption = Some(head)
  override def isEmpty = false
  override def nonEmpty = true
}

/**
 * The end of an HttpPath.
 *
 * <p>
 *   The <code>No_/</code> object represents the end of an HttpPath
 *   that is not ending in "/". For example "/foo" does not end in "/"
 *   so its end would be represented with <code>No_/</code>.
 * </p>
 */
case object No_/ extends HttpPath with HttpPathNil

/**
 * The end of an HttpPath.
 *
 * <p>
 *   The <code>Required_/</code> object represents the end of an HttpPath
 *   that is ending in "/". For example "/foo/" does end in "/"
 *   so its end would be represented with <code>Required_/</code>.
 * </p>
 */
case object Required_/ extends HttpPath with HttpPathNil
