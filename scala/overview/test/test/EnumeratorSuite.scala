package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class EnumeratorSuite extends FunSuite {
  test("enumerator |>> consume") {
    val consume = Iteratee.consume[String]()
    val enumerator = Enumerator("Foo", "Bar", "Baz")

    //
    // Enumerator#|>> works like Enumerator#apply.
    //
    //   val newIt: Future[Iteratee[String, String]] = enumerator(consume)
    //
    val newIt = enumerator |>> consume
    var result = Iteratee.flatten(newIt).run

    result.onComplete(a => Success("FooBarBaz") === a)

    Thread.sleep(100L)
  }
}
