package hector.http

import akka.actor._
import akka.pattern.ask

import hector.actor._
import hector.Hector
import hector.session._

import scala.concurrent._
import scala.concurrent.duration._

final class HttpSession(val id: String) extends Serializable {
  import Hector.session

  def set[V](key: String, value: V): Future[Unit] =
    ask(session, SessionActor.Store(id, key, value))(Hector.config.defaultSessionTimeout).mapTo[Unit]

  def get[V](key: String)(implicit manifest: Manifest[V]): Future[Option[V]] =
    apply(key)

  def update[V](key: String, value: V) {
    session ! SessionActor.Store(id, key, value)
  } 

  def apply[V](key: String)(implicit manifest: Manifest[V]): Future[Option[V]] =
    ask(session, SessionActor.Load(id, key))(Hector.config.defaultSessionTimeout).mapTo[Option[V]]

  /** When the session has been created. */
  def created: Future[Long] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    
    get[Long]("hector:session:created") map { _ getOrElse 0L }
  }

  /** When the session has been seen for the last time. */
  def lastSeen: Future[Long] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    get[Long]("hector:session:lastSeen") map { _ getOrElse 0L }
  }

  def onDestroy[U](f: => U) {
    import SignalActor._
    import hector.session.signals._

    val actor =
      Hector.system.actorOf(Props(new Actor {
        override def receive = {
          case Destroy(destroyedId) =>
            if(destroyedId == id) {
              f
              self ! PoisonPill
            }
        }
      }))

    Hector.sessionSignals ! Subscribe(actor)
  }
}
