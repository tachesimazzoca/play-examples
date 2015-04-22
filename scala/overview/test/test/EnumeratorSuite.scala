package test

import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee._
import play.api.test._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class EnumeratorSuite extends FunSuite with OneAppPerSuite {

  implicit override lazy val app = FakeApplication()

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

  test("generateM |>> Iterator.foreach") {
    val sb = new StringBuffer()
    val printSink = Iteratee.foreach[String](a => sb.append(a))
    val enumerator = Enumerator.generateM {
      Promise.timeout(Some("Foo"), 200.milliseconds)
    }
    enumerator |>> printSink

    Thread.sleep(1000L)
    assert(sb.toString.matches("^(Foo)+$"))
  }

  test("Concurrent.uncast") {
    val sb = new StringBuffer()
    val printSink = Iteratee.foreach[String](a => sb.append(a))
    val enumerator = Concurrent.unicast[String](
      onStart = { (ch) =>
        ch.push("Foo")
        ch.push("Bar")
        ch.push("Baz")
      }
    )
    enumerator |>> printSink

    Thread.sleep(500L)
    assert("FooBarBaz" === sb.toString)
  }
}
