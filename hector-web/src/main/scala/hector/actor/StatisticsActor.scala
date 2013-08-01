package hector.actor

import akka.actor.{ActorLogging, Actor}

import com.google.common.collect.HashMultimap

import hector.Hector
import hector.actor.stats.{ExceptionOccurred, RequestCompleted}
import hector.config.RunModes
import hector.util.{RMS, letItCrash}

/**
 */
final class StatisticsActor extends Actor with ActorLogging {
  //STATEFUL!

  // Variables for time-tracking operations.
  private[this] var minMs: Float = Float.MaxValue
  private[this] var maxMs: Float = Float.MinValue
  private[this] var numSamples: Int = 0
  private[this] val rms: RMS = new RMS(0x400)

  // Variables for error tracking
  private[this] val errorMap = HashMultimap.create[String, Throwable]()

  override def receive = {
    case RequestCompleted(ms) ⇒
      if(ms < minMs) { minMs = ms }
      if(ms > maxMs) { maxMs = ms }

      rms += ms.toDouble
      numSamples += 1

      if(Hector.config.runMode < RunModes.Production) {
        log.info("Completed request in {}ms. (min: {}ms, max: {}ms, rms: {}ms)", ms, minMs, maxMs, rmsMs)
      } else {
        log.debug("Completed request in {}ms. (min: {}ms, max: {}ms, rms: {}ms)", ms, minMs, maxMs, rmsMs)
      }

    case ExceptionOccurred(exception) ⇒
      errorMap.put(exception.getClass.getName, exception)

    case CreateResponse(_, _) ⇒
      import hector.http.PlainTextResponse

      letItCrash()

      val sb = new StringBuilder()
      val keys =  errorMap.keys()
      val iter = keys.iterator()
      while(iter.hasNext) {
        val key = iter.next()
        sb.append("\t"+key.toString+":\n")

        val values = errorMap.get(key)
        val iter2 = values.iterator()

        while(iter2.hasNext) {
          val value = iter2.next()
          "\t\t"+value.getMessage+"\n"
        }
      }


      sender ! PlainTextResponse(
        "min[ms]:\t"+minMs+"\n"+
        "max[ms]:\t"+maxMs+"\n"+
        "rms[ms]:\t"+rmsMs+"\n"+
        "total:\t\t"+numSamples+"\n"+
        "exceptions:\n"+sb.toString+"\n"
      )
  }

  private def rmsMs: Double = rms.toDouble
}
