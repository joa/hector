package hector.http

import akka.dispatch.Future
import akka.pattern.ask

import hector.actor._
import hector.Hector
import hector.session._

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
  def created: Future[Long] = get[Long]("hector:session:created") map { _ getOrElse 0L }

  /** When the session has been seen for the last time. */
  def lastSeen: Future[Long] = get[Long]("hector:session:lastSeen") map { _ getOrElse 0L }
}
