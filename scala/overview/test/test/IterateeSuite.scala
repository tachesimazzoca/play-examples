package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
    def folder[E, A](step: Step[E, A]): Future[Option[A]] = step match {
      case Step.Done(a, e) => Future.successful(Some(a))
      case Step.Cont(_) => Future.successful(None)
      case Step.Error(_, _) => Future.successful(None)
    }

    //
    // val doneIt = new Iteratee[String, Int] {
    //   override def fold[B](folder: (Step[String, Int]) => Future[B])
    //                       (implicit ec: ExecutionContext): Future[B] =
    //     folder(Step.Done(1, Input.Empty))
    // }
    //
    val doneIt = Done[String, Int](1, Input.Empty)
    doneIt.fold(folder).onComplete(a => assert(Success(Some(1)) === a))

    Thread.sleep(100L)
  }

  test("ContIteratee") {
    def folder[E, A](in: Input[E])(step: Step[E, A]): Future[A] = step match {
      case Step.Done(a, _) => Future.successful(a)
      case Step.Cont(k) => k(in).fold({
        case Step.Done(a1, _) => Future.successful(a1)
        case Step.Cont(_) => throw new UnsupportedOperationException()
        case Step.Error(msg, _) => throw new Error(msg)
      })
      case Step.Error(msg, _) => throw new Error(msg)
    }

    //
    // val contIt = new Iteratee[String, Int] {
    //   override def fold[B](folder: (Step[String, Int]) => Future[B])
    //                       (implicit ec: ExecutionContext): Future[B] =
    //     folder(Step.Cont {
    //       case Input.El(e) => Done(e.toInt, Input.EOF)
    //       case Input.Empty => Error("Input.Empty not supported", Input.EOF)
    //       case Input.EOF => Done(0, Input.EOF)
    //     })
    // }
    //
    val contIt = Cont[String, Int] {
      case Input.El(e) => Done(e.toInt, Input.EOF)
      case Input.Empty => Error("Input.Empty not supported", Input.EOF)
      case Input.EOF => Done(0, Input.EOF)
    }
    contIt.fold(folder(Input.EOF)).onComplete(a => assert(Success(0) === a))
    contIt.fold(folder(Input.El("123"))).onComplete(a => assert(Success(123) === a))

    Thread.sleep(100L)
  }

  test("inputLength") {
    val inputLength = Iteratee.fold[Array[Byte], Int](0) { (acc, x) =>
      acc + x.length
    }

    val f = for {
      it1 <- inputLength.feed(Input.El("ab".getBytes))
      it2 <- it1.feed(Input.El("cde".getBytes))
      len <- it2.run
    } yield len
    f.onComplete(a => assert(Success(5) === a))

    Thread.sleep(100L)
  }

  test("consume") {
    //
    // val consume = Iteratee.fold[String, String]("") { (acc, chunk) =>
    //   acc ++ chunk
    // }
    //
    val consume = Iteratee.consume[String]()

    val f = for {
      it1 <- consume.feed(Input.El("Hello"))
      it2 <- it1.feed(Input.El(" Iteratee!"))
      len <- it2.run
    } yield len
    f.onComplete(a => assert(Success("Hello Iteratee!") === a))

    Thread.sleep(100L)
  }
}