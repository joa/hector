package hector.microbenchmark

import hector.microbenchmark.util.classesOf

import com.google.caliper.{SimpleBenchmark, Runner}
import java.lang.reflect.Modifier

/**
 */
object AllBenchmarks {
  def main(args: Array[String]) {
    val benchmarks =
      for {
        klass ← classesOf("hector.microbenchmark") if klass.getName.endsWith("Benchmark") && !Modifier.isAbstract(klass.getModifiers)
      } yield {
        klass.asInstanceOf[Class[SimpleBenchmark]]
      }

    val total = benchmarks.length
    var current = 0

    println("Benchmarks:")
    benchmarks foreach { klass ⇒ println("  "+klass.getName) }

    for {
      benchmark ← benchmarks
    } {
      println("Running Benchmark "+benchmark.getName+" ...")

      //TODO(joa): need to fork java procecss
      Runner.main(benchmark, args)
      current += 1

      println("Benchmark Complete ("+current+"/"+total+")")
    }
  }
}
