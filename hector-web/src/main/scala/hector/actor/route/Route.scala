package hector.actor.route

import akka.actor.ActorSelection
import akka.util.Timeout
import hector.Hector
import hector.http.HttpResponse


/**
 * The Route reply.
 *
 * <p>A route defines an actor and additional arguments it will receive.</p>
 *
 * @param target The target actor.
 * @param arguments The arguments for the actor.
 * @param timeout The timeout for this route.
 * @param recover The recovery strategy.
 * @tparam A Type of arguments.
 */
final case class Route[A](
  target: ActorSelection,
  arguments: Option[A] = None,
  timeout: Timeout = Hector.config.defaultRequestTimeout,
  recover: Option[PartialFunction[Throwable, HttpResponse]] = None
)
