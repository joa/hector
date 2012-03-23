package hector.session

import hector.http.HttpRequest

import akka.dispatch.Future

/**
 * @author Joa Ebert
 */
trait SessionBackend {
  def store[V <: Serializable](request: HttpRequest, key: String, value: V): Future[Unit]

  def load[V <: Serializable](request: HttpRequest, key: String): Future[Option[V]]
}
