package hector.microbenchmark

import hector.microbenchmark.util.HectorBenchmark

/**
 */
final class EscapeJavaStringBenchmark extends HectorBenchmark {
  private[this] var chars: String  = _

  override protected def setUp() {
    chars =
      (for {
        i <- 0x00000 until 0x10000
      } yield i.asInstanceOf[Char]) mkString ""
  }

  def timeEscapeJavaString(reps: Int) = {
    benchmark(reps) {
      hector.util.escapeJavaScriptString(chars)
    }
  }
}
