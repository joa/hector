package hector.actor

import akka.actor.Actor
import hector.util.{RMS, letItCrash}


object StatisticsActor {
  final case class RequestCompleted(ms: Float)
}

/**
 */
final class StatisticsActor extends Actor {
  import StatisticsActor._

  //STATEFUL!

  private[this] var minMs: Float = Float.MaxValue
  private[this] var maxMs: Float = Float.MinValue
  private[this] var numSamples: Int = 0
  private[this] val rms: RMS = new RMS(0x400)

  override protected def receive = {
    case RequestCompleted(ms) ⇒
      if(ms < minMs) { minMs = ms }
      if(ms > maxMs) { maxMs = ms }

      rms += ms.toDouble
      numSamples += 1

      println("Completed request in "+ms+"ms. (min: "+minMs+"ms, max: "+maxMs+"ms, rms: "+rmsMs+"ms)")

    case CreateResponse(request, _) ⇒
      import hector.http.PlainTextResponse

      letItCrash()

      sender ! PlainTextResponse(
        "min[ms]:\t"+minMs+"\n"+
        "max[ms]:\t"+maxMs+"\n"+
        "rms[ms]:\t"+rmsMs+"\n"+
        "total:\t"+numSamples+"\n"
      )
  }

  private def rmsMs: Double = rms.toDouble
}
