package hector.config

/**
 */
sealed trait RunMode {
  /** The priority of a mode. Production has highest priority */
  def priority: Int

  def <(that: RunMode): Boolean = this.priority < that.priority
  def <=(that: RunMode): Boolean = this.priority <= that.priority
  def >(that: RunMode): Boolean = this.priority > that.priority
  def >=(that: RunMode): Boolean = this.priority >= that.priority
}

object RunModes {
  case object Production extends RunMode { override def priority = 3 }
  case object Staging extends RunMode { override def priority = 2 }
  case object Testing extends RunMode { override def priority = 1 }
  case object Development extends RunMode { override def priority = 0 }
}
