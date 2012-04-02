package hector.microbenchmark

import hector.microbenchmark.util.classesOf

import com.google.caliper.{SimpleBenchmark, Runner}

/**
 */
object AllBenchmarks {
  def main(args: Array[String]) {
    val benchmarks =
      for {
        klass <- classesOf("hector.microbenchmark") if klass.getName.endsWith("Benchmark")
      } yield {
        klass.asInstanceOf[Class[SimpleBenchmark]]
      }

    println("Benchmarks:")
    benchmarks foreach { klass => println("  "+klass.getName) }

    for {
      benchmark <- benchmarks
    } {
      println("Running Benchmark "+benchmark.getName+":")
      Runner.main(benchmark, args)
    }
  }
}
