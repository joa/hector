package hector.session

import hector.http.HttpRequest

import akka.dispatch.Future

/**
 */
trait SessionBackend {
  def store[V](id: String, key: String, value: V): Future[Unit]

  def load[V](id: String, key: String): Future[Option[V]]
}
