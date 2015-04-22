package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class EnumerateeSuite extends FunSuite {
  test("&>> is transform") {
    val consume = Iteratee.consume[String]()

    val bytes = Enumerator("Enumeratee".getBytes: _*)
    val byteToHexStr = Enumeratee.map[Byte] { b =>
      "%02X".format(b)
    }

    //val result = bytes |>>> byteToHexStr.transform(consume)
    val result = bytes |>>> (byteToHexStr &>> consume)
    result.onComplete(a => assert(Success("456E756D657261746565") === a))

    Thread.sleep(100L)
  }

  test("with Enumerator#(through|&>)") {
    val sum = Iteratee.fold[Int, Int](0) { (acc, x) =>
      acc + x
    }

    val strings = Enumerator("1", "2", "3")
    val strToInt = Enumeratee.map[String](_.toInt)

    //val result = strings.through(strToInt) |>>> sum
    val result = (strings &> strToInt) |>>> sum
    result.onComplete(a => assert(Success(6) === a))

    Thread.sleep(100L)
  }

  test("different input") {
    val sum = Iteratee.fold[Int, Int](0) { (acc, x) =>
      acc + x
    }
    val strToInt = Enumeratee.map[String](_.toInt)

    val adaptedIt = strToInt(sum)
    val originalIt = Iteratee.flatten(Enumerator("1", "2") |>>> adaptedIt)
    val result = Enumerator(3, 4, 5) |>>> originalIt
    result.onComplete(a => assert(Success(15) === a))

    Thread.sleep(100L)
  }
}
