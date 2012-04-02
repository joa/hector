package hector.microbenchmark

import com.google.caliper.{Runner, SimpleBenchmark}


object EscapeJavaStringBenchmark {
  def main(args: Array[String]) {
    Runner.main(classOf[EscapeJavaStringBenchmark], args)
  }
}

/**
 */
final class EscapeJavaStringBenchmark extends SimpleBenchmark {
  def timeEscapeEmptyJavaString(reps: Int) {
    var i = 0
    while(i < reps) {
      hector.util.escapeJavaScriptString("")
      i += 1
    }
  }
}
