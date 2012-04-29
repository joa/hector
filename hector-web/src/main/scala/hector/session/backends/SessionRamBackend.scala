package hector.session.backends

import hector.session.SessionBackend
import hector.http.HttpRequest

import akka.dispatch._

/**
 */
final class SessionRamBackend(private[this] val context: ExecutionContext) extends SessionBackend {
  //XXX(joa): how does the scala wrapper for ConcurrentHashMap behave exactly? is this correct?

  import collection.mutable.ConcurrentMap

  private[this] implicit val implicitContext = context

  private[this] val map: ConcurrentMap[String, ConcurrentMap[String, Any]] = {
    import java.util.concurrent.{ConcurrentHashMap ⇒ JConcurrentHashMap}
    import scala.collection.JavaConversions.asScalaConcurrentMap

    asScalaConcurrentMap(new JConcurrentHashMap[String, ConcurrentMap[String, Any]]())
  }

  override def store[V](id: String, key: String, value: V) =
    Future {
      import java.util.concurrent.{ConcurrentHashMap ⇒ JConcurrentHashMap}
      import scala.collection.JavaConversions.asScalaConcurrentMap

      // Create a new Map in case we need it.
      val newMap = asScalaConcurrentMap(new JConcurrentHashMap[String, Any]())

      val sessionValues =
        map.putIfAbsent(id, newMap) match {
          case Some(existing) ⇒ existing
          case None ⇒ newMap
        }

      sessionValues.put(key, value)

      ()
    }

  override def load[V](id: String, key: String) =
    Future {
      val sessionValues = map.get(id)
      val sessionValue = sessionValues flatMap { _.get(key) }

      sessionValue map { _.asInstanceOf[V] }
    }
}
