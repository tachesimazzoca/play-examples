package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class IterateeSuite extends FunSuite {
  test("Input") {
    val f = (n: Int) => s"Hello${n}"
    assert(Input.El("Hello123") === Input.El(123).map(f))
    assert(Input.Empty === Input.Empty.map(f))
    assert(Input.EOF === Input.EOF.map(f))
  }

  test("doneIteratee") {
    def f(step: Step[String, Int]): Future[Option[Int]] = step match {
      case Step.Done(a, e) => Future(Some(a))
      case Step.Cont(k) => Future(None)
      case Step.Error(msg, e) => Future(None)
    }

    val doneIteratee = new Iteratee[String, Int] {
      override def fold[B](folder: (Step[String, Int]) => Future[B])
                          (implicit ec: ExecutionContext): Future[B] =
        folder(Step.Done(1, Input.Empty))
    }
    doneIteratee.fold(f).onComplete(a => println(a))
    Thread.sleep(100L)
  }
}
