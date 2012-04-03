package hector.microbenchmark.util

import com.google.caliper.SimpleBenchmark

/**
 */
abstract class HectorBenchmark extends SimpleBenchmark {
  protected def benchmark[@specialized U](reps: Int)(f: ⇒ U) = {
    var result = 0
    var i = 0

    while(i < reps) {
      val value = f

      if(value != null) {
        result += value.hashCode()
      }

      i += 1
    }

    result
  }
}
