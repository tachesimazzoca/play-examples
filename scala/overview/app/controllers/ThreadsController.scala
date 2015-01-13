package controllers

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Random

object ThreadsController extends Controller {
  //
  // You can’t magically turn synchronous IO into asynchronous by wrapping
  // it in a Future. If you can’t change the application’s architecture to
  // avoid blocking operations, at some point that operation will have to
  // be executed, and that thread is going to block. So in addition to
  // enclosing the operation in a Future, it’s necessary to configure it
  // to run in a separate execution context that has been configured with
  // enough threads to deal with the expected concurrency.
  //
  // See "Understanding Play thread pools" for more information.
  //   https://www.playframework.com/documentation/2.3.x/ThreadPools
  //
  private def blockingFuture(msec: Long, request: Request[AnyContent])
                            (implicit context: ExecutionContext): Future[Result] = {
    val tag = Integer.toHexString(System.identityHashCode(request))
    Future {
      val thread = Thread.currentThread();
      Logger.info(s"Start blocking ${thread}: ${tag}")
      Thread.sleep(msec)
      Logger.info(s"Finish blocking ${thread}: ${tag}")
      Ok(s"Done after ${msec} msec: ${tag}")
    } (context)
  }

  def defaultAction = Action.async { request =>
    val msec = request.getQueryString("msec").map(_.toLong).getOrElse(0L)
    blockingFuture(msec, request)
  }

  def singleAction = Action.async { request =>
    val msec = request.getQueryString("msec").map(_.toLong).getOrElse(0L)
    blockingFuture(msec, request)(Contexts.singleThread)
  }

  def expensiveAction = Action.async { request =>
    val msec = request.getQueryString("msec").map(_.toLong).getOrElse(5000L)
    blockingFuture(msec, request)(Contexts.expensiveOperations)
  }

  def synchronousAction = Action.async { request =>
    val msec = request.getQueryString("msec").map(_.toLong).getOrElse(500L)
    blockingFuture(msec, request)(Contexts.synchronousOperations)
  }

  def timeoutAction = Action.async {
    val n1 = Random.nextInt(2) * 5
    val n2 = Random.nextInt(2) * 5

    val job1 = Promise.timeout({
      Logger.info(s"Done job1 after ${n1} sec")
      ("job1", (n1, n2))
    }, n1.second)
    val job2 = Promise.timeout({
      Logger.info(s"Done job2 after ${n2} sec")
      ("job2", (n1, n2))
    }, n2.second)

    Future.firstCompletedOf(Seq(job1, job2)) map { case (job, ns) =>
      Ok(s"The first completed job is ${job}. ${ns}")
    }
  }
}
