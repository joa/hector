package hector.http.io

/**
 */
object implicits {
  implicit def arrayToImmutableByteArray(array: Array[Byte]): ImmutableByteArray = ImmutableByteArray(array)

  implicit def immutableByteArrayToArray(immutableByteArray: ImmutableByteArray): Array[Byte] = immutableByteArray.toArray
}
