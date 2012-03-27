package hector

import com.google.common.base.Strings

package object util {
  def convertBytesToHexString(hash: Array[Byte]): String =
    convertBytesToHexString(new StringBuilder(hash.length << 1), hash)

  def convertBytesToHexString(stringBuilder: StringBuilder, hash: Array[Byte]): String = {
    var i = 0
    val n = hash.length

    while(i < n) {
      val value = hash(i) & 0xff

      if(value < 0x10) {
        stringBuilder append '0'
      }

      stringBuilder append Integer.toString(value, 16)

      i += 1
    }

    stringBuilder.toString()
  }

  private[this] val LetItCrashEnabled = false

  def letItCrash(probability: Double = 0.06125) {
    if(LetItCrashEnabled && math.random < probability) {
      println("Simulated exception.")
      throw new RuntimeException("Let it Crash!")
    }
  }

  def trimToOption(value: String): Option[String] =
    if(null == value || value.length == 0) {
      None
    } else {
      val trimmedString = value.trim

      if(trimmedString.length == 0) {
        None
      } else {
        Some(trimmedString)
      }
    }
}
