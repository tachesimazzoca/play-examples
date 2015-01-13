package controllers

import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext

object Contexts {
  import play.api.Play.current

  implicit val singleThread: ExecutionContext =
    Akka.system.dispatchers.lookup("contexts.single-thread")

  implicit val expensiveOperations: ExecutionContext =
    Akka.system.dispatchers.lookup("contexts.expensive-operations")

  implicit val synchronousOperations: ExecutionContext =
    Akka.system.dispatchers.lookup("contexts.synchronous-operations")
}
