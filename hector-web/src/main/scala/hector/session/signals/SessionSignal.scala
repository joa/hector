package hector.session.signals

sealed trait SessionSignal

/**
 * Message indicating a session is about to be created.
 *
 * This does not necessarily mean it exists yet.
 */
case class Create(id: String) extends SessionSignal

/**
 * Message indicating a session is about to be destroyed.
 *
 * This does not necessarily mean the session has been destroyed already.
 */
case class Destroy(id: String) extends SessionSignal