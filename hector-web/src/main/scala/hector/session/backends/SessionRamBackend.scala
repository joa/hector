package hector.session.backends

import akka.dispatch._
import akka.util.Duration

import com.google.common.cache.{LoadingCache, CacheBuilder, CacheLoader}

import hector.Hector
import hector.http.HttpRequest
import hector.session.SessionBackend

import java.util.concurrent.{Callable => JCallable}

import scala.collection.mutable.ConcurrentMap
import scala.compat.Platform

/**
 * The SessionRamBackend is a SessionBackend implementation for development.
 * 
 * <p>Session's are put into a cache with a maximum number of entries. This means
 * a session might be evicted prior to it's end of life.</p>
 *
 * <p>All session data is kept in memory.</p>
 */
final class SessionRamBackend(private[this] val context: ExecutionContext, maxNrOfSessions: Int = 10000) extends SessionBackend {

  type Session = ConcurrentMap[String, Any]

  private[this] implicit val implicitContext = context

  private[this] val cache: LoadingCache[String, Session] = {
    val cacheBuilder = 
      (CacheBuilder.newBuilder().asInstanceOf[CacheBuilder[String, Session]])

    val cacheLoader = 
      new CacheLoader[String, Session] {
        override def load(key: String): Session = {
          import java.util.concurrent.{ConcurrentHashMap ⇒ JConcurrentHashMap}
          import scala.collection.JavaConversions.asScalaConcurrentMap
          
          val currentTime = Platform.currentTime

          val map = 
            asScalaConcurrentMap(new JConcurrentHashMap[String, Any](2, 0.75f, 1))

          //TODO(joa): those strings must be constants
          map.put("hector:session:created", currentTime)
          map.put("hector:session:lastSeen", currentTime)
          map
        }
      }

    val Duration(lifetime, lifetimeUnit) = Hector.config.sessionLifetime

    cacheBuilder.
      maximumSize(maxNrOfSessions).
      expireAfterAccess(lifetime, lifetimeUnit).
      build(cacheLoader)
  }

  override def store[V](id: String, key: String, value: V) = {
    val session = cache.getUnchecked(id)

    session.put(key, value)

    Promise.successful(())
  }

  override def load[V](id: String, key: String) = {
    val session = cache.getUnchecked(id)
    val value = session.get(key) map { _.asInstanceOf[V] }

    Promise.successful(value)
  }
}
