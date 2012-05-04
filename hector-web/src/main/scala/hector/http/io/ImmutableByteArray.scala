package hector.http.io

import compat.Platform

object ImmutableByteArray {
  /**
   * Creates and returns an ImmutableByteArray.
   *
   * <p>A defensive copy of the passed value will be created in order to ensure immutability.</p>
   *
   * @param value The array to wrap.
   * @return An ImmutableByteArray for the given value.
   */
  def apply(value: Array[Byte]): ImmutableByteArray = {
    val defensiveCopy: Array[Byte] = new Array[Byte](value.length)

    Platform.arraycopy(value, 0, defensiveCopy, 0, value.length)

    new ImmutableByteArray(defensiveCopy)
  }

  /**
   * Creates and returns an unsafe view for a given array of bytes.
   *
   * <p>The difference between <code>unsafe</code> and the <code>apply</code> is that no defensive
   * copy is being created in <code>unsafe</code>.
   *
   * @param value The array to wrap.
   * @return An ImmutableByteArray for the given value.
   */
  def unsafe(value: Array[Byte]): ImmutableByteArray =
    new ImmutableByteArray(value)
}

/**
 */
final class ImmutableByteArray(private[io] val backingArray: Array[Byte]) extends Serializable with Immutable {
  /**
   * Copy of the backing array.
   */
  def toArray: Array[Byte] = {
    val result: Array[Byte] = new Array[Byte](backingArray.length)
    Platform.arraycopy(backingArray, 0, result, 0, backingArray.length)
    result
  }
}
