package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class IterateeSuite extends FunSuite {
  test("Input") {
    val f = (n: Int) => s"Hello${n}"
    assert(Input.El("Hello123") === Input.El(123).map(f))
    assert(Input.Empty === Input.Empty.map(f))
    assert(Input.EOF === Input.EOF.map(f))
  }

  test("DoneIteratee") {
    def folder(step: Step[String, Int]): Future[Option[Int]] = step match {
      case Step.Done(a, e) => Future(Some(a))
      case Step.Cont(k) => Future(None)
      case Step.Error(msg, e) => Future(None)
    }

    val doneIt = new Iteratee[String, Int] {
      override def fold[B](folder: (Step[String, Int]) => Future[B])
                          (implicit ec: ExecutionContext): Future[B] =
        folder(Step.Done(1, Input.Empty))
    }
    doneIt.fold(folder).onComplete(a => assert(Success(Some(1)) === a))

    val doneIt2 = Done[String, Int](2, Input.Empty)
    doneIt2.fold(folder).onComplete(a => assert(Success(Some(2)) === a))

    Thread.sleep(100L)
  }

  test("ContIteratee") {
    def folder(step: Step[String, Int]): Future[Int] = step match {
      case Step.Done(a, e) => Future(a)
      case Step.Cont(k) => k(Input.EOF).fold({
        case Step.Done(a1, _) => Future.successful(a1)
        case _ => throw new UnsupportedOperationException()
      })
      case _ => throw new UnsupportedOperationException()
    }

    val contIt = new Iteratee[String, Int] {
      override def fold[B](folder: (Step[String, Int]) => Future[B])
                          (implicit ec: ExecutionContext): Future[B] =
        folder(Step.Cont {
          case Input.El(e) => Done(e.toInt, Input.EOF)
          case Input.Empty => this
          case Input.EOF => Done(0, Input.EOF)
        })
    }
    contIt.fold(folder).onComplete(a => assert(Success(0) === a))

    val contIt2 = Cont[String, Int](in => Done(100, Input.Empty))
    contIt2.fold(folder).onComplete(a => assert(Success(100) === a))

    Thread.sleep(100L)
  }
}
