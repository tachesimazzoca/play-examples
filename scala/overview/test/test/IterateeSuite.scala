package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class IterateeSuite extends FunSuite with ScalaFutures {
  test("Input") {
    val f = (n: Int) => s"Hello${n}"
    assert(Input.El("Hello123") === Input.El(123).map(f))
    assert(Input.Empty === Input.Empty.map(f))
    assert(Input.EOF === Input.EOF.map(f))
  }

  test("DoneIteratee") {
    val doneIt = Done[String, Int](1, Input.Empty)
    whenReady(Iteratee.flatten(doneIt.feed(Input.El("deadbeef"))).run) { a =>
      assert(1 === a)
    }
    whenReady(Iteratee.flatten(doneIt.feed(Input.Empty)).run) { a =>
      assert(1 === a)
    }
    whenReady(Iteratee.flatten(doneIt.feed(Input.EOF)).run) { a =>
      assert(1 === a)
    }

    def folder[E, A](step: Step[E, A]): Future[Option[A]] = step match {
      case Step.Done(a, _) => Future.successful(Some(a))
      case Step.Cont(_) => Future.successful(None)
      case Step.Error(_, _) => Future.successful(None)
    }
    val it = new Iteratee[String, Int] {
      override def fold[B](folder: (Step[String, Int]) => Future[B])
                          (implicit ec: ExecutionContext): Future[B] =
        folder(Step.Done(1, Input.Empty))
    }
    whenReady(it.fold(folder)) { a =>
      assert(Some(1) === a)
    }
  }

  test("ContIteratee") {
    def step(acc: Int)(in: Input[String]): Iteratee[String, Int] = in match {
      case Input.El(e: String) => Cont(step(acc + e.toInt))
      case Input.Empty => Cont(step(acc))
      case Input.EOF => Done(acc, Input.EOF)
    }

    val contIt = Cont[String, Int](step(0))
    Iteratee.flatten(contIt.feed(Input.EOF)).run
      .onComplete(a => assert(Success(0) === a))
    Iteratee.flatten(contIt.feed(Input.El("123"))).run
      .onComplete(a => assert(Success(123) === a))

    whenReady(for {
      it1 <- contIt.feed(Input.El("12"))
      it2 <- it1.feed(Input.El("34"))
      it3 <- it2.feed(Input.Empty)
      it4 <- it3.feed(Input.El("56"))
      x <- it4.run
    } yield x) { a =>
      assert(12 + 34 + 56 === a)
    }

    val onetimeIt = new Iteratee[String, Int] {
      override def fold[B](folder: (Step[String, Int]) => Future[B])
                          (implicit ec: ExecutionContext): Future[B] =
        folder(Step.Cont {
          case Input.El(e) => Done(e.toInt, Input.EOF)
          case Input.Empty => this
          case Input.EOF => Done(0, Input.EOF)
        })
    }

    whenReady(for {
      it1 <- onetimeIt.feed(Input.El("12"))

      // The following feed will be ignored because it just
      // returns Done(a, e) regardless of the type of input.
      it2 <- it1.feed(Input.El("34"))
      it3 <- it2.feed(Input.El("56"))

      x <- it3.run
    } yield x) { a =>
      assert(12 === a)
    }

    // fold
    def folder[E, A](in: Input[E])(step: Step[E, A]): Future[A] = step match {
      case Step.Done(a, _) => Future.successful(a)
      case Step.Cont(k) => k(in).fold({
        case Step.Done(a1, _) => Future.successful(a1)
        case Step.Cont(_) => throw new UnsupportedOperationException()
        case Step.Error(msg, _) => throw new Error(msg)
      })
      case Step.Error(msg, _) => throw new Error(msg)
    }
    whenReady(onetimeIt.fold(folder(Input.EOF))) { a =>
      assert(0 === a)
    }
    whenReady(onetimeIt.fold(folder(Input.El("123")))) { a =>
      assert(123 === a)
    }
  }

  test("ErrorIteratee") {
    def step(acc: Int)(in: Input[String]): Iteratee[String, Int] = in match {
      case Input.El(e: String) =>
        if (!e.isEmpty) Cont(step(acc + e.toInt))
        else Error("empty string", Input.Empty)
      case Input.Empty => Cont(step(acc))
      case Input.EOF => Done(acc, Input.EOF)
    }

    val errorIt = Cont[String, Int](step(0))
    intercept[java.lang.RuntimeException] {
      whenReady(Iteratee.flatten(errorIt.feed(Input.El(""))).run) { _ =>}
    }

    intercept[java.lang.RuntimeException] {
      whenReady(for {
        it1 <- errorIt.feed(Input.El("12"))
        it2 <- it1.feed(Input.El(""))

        // The following feed will be ignored because it just
        // returns Error(msg, e) regardless of the type of input.
        it3 <- it2.feed(Input.El("56"))
        it4 <- it3.feed(Input.Empty)

        a <- it4.run
      } yield a) { _ =>}
    }
  }

  test("inputLength") {
    val inputLength = Iteratee.fold[Array[Byte], Int](0) { (acc, x) =>
      acc + x.length
    }

    whenReady(for {
      it1 <- inputLength.feed(Input.El("ab".getBytes))
      it2 <- it1.feed(Input.El("cde".getBytes))
      len <- it2.run
    } yield len) { a =>
      assert(5 === a)
    }
  }

  test("consume") {
    //
    // val consume = Iteratee.fold[String, String]("") { (acc, chunk) =>
    //   acc ++ chunk
    // }
    //
    val consume = Iteratee.consume[String]()

    whenReady(for {
      it1 <- consume.feed(Input.El("Hello"))
      it2 <- it1.feed(Input.El(" Iteratee!"))
      len <- it2.run
    } yield len) { a =>
      assert("Hello Iteratee!" === a)
    }
  }

  test("foreach") {
    val sb = new StringBuffer()
    val printIt = Iteratee.foreach[String](sb.append(_))
    printIt.feed(Input.El("foo"))
    printIt.feed(Input.El("bar"))
    Thread.sleep(100L)
    assert("foobar" === sb.toString())
  }

  test("flatten") {
    val consume = Iteratee.consume[String]()
    val newIt = consume.feed(Input.El("foo")).flatMap(_.feed(Input.El("bar")))
    val futureIt = Iteratee.flatten(newIt)
    whenReady(futureIt.run) { a =>
      assert("foobar" === a)
    }
  }
}
