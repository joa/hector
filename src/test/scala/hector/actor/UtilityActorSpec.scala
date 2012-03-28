package hector.actor

import hector.http.{No_/, HttpPath}
import hector.{Hector, HectorSpec}

import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.actor.{ActorRef, Props}

/**
 */
final class UtilityActorSpec extends HectorSpec {
  import UtilityActor._

  private[this] def utilityActor = Hector.system.actorOf(Props[UtilityActor])

  private[this] implicit val timeout = Timeout(1.second)

  describe("UtilityActor.NewRandomHash") {
    it("replies with 32-byte long hashes") {
      val future =
        (utilityActor ? NewRandomHash).mapTo[String]

      val hash =
        Await.result(future, 1.second)

      hash must have length (32)
    }

    it("replies unique hashes") {
      val hashes = new Array[String](1000)

      fillArray(hashes, utilityActor, NewRandomHash)

      mustNotContainDuplicates(hashes)
    }
  }

  describe("UtilityActor.NewFunctionName") {
    it("replies with a 33 character long function name") {
      val future =
        (utilityActor ? NewFunctionName).mapTo[String]

      val functionName =
        Await.result(future, 1.second)

      functionName must have length (33)
    }

    it("replies with a function name that begins with an underscore") {
      val future =
        (utilityActor ? NewFunctionName).mapTo[String]

      val functionName =
        Await.result(future, 1.second)

      functionName must startWith ("_")
    }

    it("replies unique function names") {
      val names = new Array[String](1000)

      fillArray(names, utilityActor, NewFunctionName)

      mustNotContainDuplicates(names)
    }
  }

  private[this] def fillArray[A](array: Array[A], actor: ActorRef, message: Any)(implicit m: Manifest[A]) {
    for { i ← 0 until array.length } {
      array(i) =
        Await.result(
          (actor ? message).mapTo[A],
          1.second
        )
    }
  }

  private[this] def mustNotContainDuplicates[A](array: Array[A]) {
    var i = 0
    val n = array.length

    while(i < n) {
      var j = i + 1

      while(j < n) {
        array(i) must not equal (array(j))

        j += 1
      }

      i += 1
    }
  }
}
