package hector.actor

import hector.http.HttpRequest

/**
 * The CreateResponse message is sent by Hector to an Actor in order to create a response
 * for a given request.
 */
final case class CreateResponse[T](request: HttpRequest, arguments: Option[T])
