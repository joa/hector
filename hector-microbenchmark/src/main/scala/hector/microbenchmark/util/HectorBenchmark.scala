package hector.microbenchmark.util

import com.google.caliper.SimpleBenchmark

/**
 */
abstract class HectorBenchmark extends SimpleBenchmark {
  protected def benchmark[@specialized U](reps: Int)(f: => U) = {
    var result: U = null.asInstanceOf[U]
    var i = 0

    while(i < reps) {
      val value = f
      if(f != result) result = f
      i += 1
    }

    result
  }
}
