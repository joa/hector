package hector.actor

import akka.actor.Actor

import hector.util.letItCrash


object StatisticsActor {
  final case class RequestCompleted(ms: Float)
}

/**
 * @author Joa Ebert
 */
final class StatisticsActor extends Actor {
  import StatisticsActor._

  //STATEFUL!

  private[this] var minMs: Float = Float.MaxValue
  private[this] var maxMs: Float = Float.MinValue
  private[this] var sumOfSquares: Float = 0.0f
  private[this] var numSamples: Int = 0

  override protected def receive = {
    case RequestCompleted(ms) ⇒
      if(ms < minMs) { minMs = ms }
      if(ms > maxMs) { maxMs = ms }

      sumOfSquares += ms * ms
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

  private def rmsMs = math.sqrt((1.0f / numSamples.toFloat) * sumOfSquares).toFloat
}
