package hector


import hector.actor.CreateResponse
import hector.actor.route.Route
import hector.http.HttpResponse

import akka.actor.{PoisonPill, Props, Actor}

/**
 */
package object pattern {
  /**
   * Creates a response for a given request.
   *
   * <p>The <code>respond</code> method is helpful during development. It is very
   * important that one avoids to use any outside references in the response.
   * The anonymous actor might be part at of any node in the cluster.</p>
   *
   * <p>Since the anonymous actor always sends a poison pill to itself each response comes
   * with the overhead of creating and destroying an actor.</p>
   *
   * @param response The response to generate.
   * @tparam R The type of the response.
   *
   * @return A route to an anonymous actor.
   */
  def respond[R <% HttpResponse](response: R): Route[Any] = {
    val actor =
      Hector.system.actorOf(Props(new Actor {
          override def receive = {
            case CreateResponse(_, _) ⇒
              sender ! response
              self ! PoisonPill
          }
        }
      ))

    val selection =
      Hector.system.actorSelection(actor.path)

    Route[Any](selection)
  }
}
