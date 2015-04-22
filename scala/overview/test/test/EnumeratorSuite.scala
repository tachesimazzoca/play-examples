package test

import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class EnumeratorSuite extends FunSuite {
  test("|>> is apply") {
    val consume = Iteratee.consume[String]()
    val enumerator = Enumerator("Foo", "Bar", "Baz")

    //val newIt: Future[Iteratee[String, String]] = enumerator(consume)
    val newIt = enumerator |>> consume

    //val result = newIt.flatMap(_.run)
    val result = Iteratee.flatten(newIt).run
    result.onComplete(a => assert(Success("FooBarBaz") === a))

    Thread.sleep(100L)
  }

  test("|>>> is apply(i).flatMap(_.run)") {
    val consume = Iteratee.consume[String]()

    //val enumerator = Enumerator("Run", " ", "fluently!")
    //val result = enumerator(consume).flatMap(_.run)
    val result = Enumerator("Run", " ", "fluently!") |>>> consume
    result.onComplete(a => assert(Success("Run fluently!") === a))

    Thread.sleep(100L)
  }

  test(">>> is andThen") {
    val consume = Iteratee.consume[String]()

    //val enumerator = Enumerator("He", "ll", "o").andThen(Enumerator(" ", "Enum", "erator!"))
    val enumerator = Enumerator("He", "ll", "o") >>> Enumerator(" ", "Enum", "erator!")
    val result = enumerator |>>> consume
    result.onComplete(a => assert(Success("Hello Enumerator!") === a))

    Thread.sleep(100L)
  }

  test("fromStream") {
    val inputLength = Iteratee.fold[Array[Byte], Int](0) { (acc, x) =>
      // println(x)
      acc + x.size
    }

    val chunkSize = 4
    val enumerator = Enumerator.fromStream(
      new ByteArrayInputStream("Hello fromStream".getBytes), chunkSize)
    val result = enumerator |>>> inputLength
    result.onComplete(a => assert(Success(16) === a))

    Thread.sleep(100L)
  }
}
