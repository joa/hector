package hector.util

import javax.annotation.concurrent.NotThreadSafe

/**
 */
@NotThreadSafe
final class RMS(size: Int) {
  private[this] val values: Array[Double] = new Array[Double](size)
  private[this] var index: Int = 0
  private[this] val reciprocalSize: Double = 1.0 / size.toDouble

  def add(value: Double) {
    values(index) = value
    index = (index + 1) % size
  }

  def +=(value: Double) {
    add(value)
  }

  def toDouble = compute()

  def compute(): Double = {
    val n = size // get rid of getfield opcode
    var sumOfSquares = 0.0
    var i = 0

    while(i < n) {
      val value = values(i)

      sumOfSquares += value * value

      i += 1
    }

    math.sqrt(reciprocalSize * sumOfSquares)
  }
}
