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

  test("apply") {
    val sum = Iteratee.fold[Int, Int](0) { (acc, x) =>
      acc + x
    }
    val strToInt = Enumeratee.map[String](_.toInt)

    val doneIt = Iteratee.flatten(Enumerator(1, 2) >>> Enumerator.eof |>> sum)
    // The iteratee doneIt has been done,
    Iteratee.isDoneOrError(doneIt).onComplete(a => assert(a === Success(true)))
    val transformedIt = strToInt &>> doneIt
    // so any inputs after that will be ignored.
    (Enumerator("3", "4", "5") |>>> transformedIt).onComplete(a =>
      assert(Success(3) === a))

    // The method apply returns Iteratee[String, Iteratee[Int, Int]]
    val adaptedIt = strToInt(sum)
    // so we can get the original iteratee after the adaptedIt is done.
    val originalIt = Iteratee.flatten(Enumerator("1", "2") |>>> adaptedIt)
    // The original iteratee has not been done yet because it's just
    // an output of the adaptedIt.
    Iteratee.isDoneOrError(originalIt).onComplete(a => assert(a === Success(false)))
    val result = Enumerator(3, 4, 5) |>>> originalIt
    result.onComplete(a => assert(Success(15) === a))

    Thread.sleep(100L)
  }

  test("mapInput") {
    val sum = Iteratee.fold[Int, Int](0) { (acc, x) =>
      acc + x
    }
    val inputToInt = Enumeratee.mapInput[String] {
      case Input.El("END") => Input.EOF
      case other => other.map(_.toInt)
    }

    // 4 and 5 will be ignored
    val result = Enumerator("1", "2", "3", "END", "4", "5") &> inputToInt |>>> sum
    result.onComplete(a => assert(Success(6) === a))

    Thread.sleep(100L)
  }

  test("Traversable") {
    val it = Iteratee.fold[Array[Byte], String]("") { (acc, x) =>
      acc ++ x.map(_.toChar).mkString("")
    }

    val enumerator = Enumerator(
      "123".getBytes(),
      "456".getBytes(),
      "789".getBytes()
    )

    def limitChunks(n: Int) = {
      Enumeratee.take[Array[Byte]](n)
    }
    (enumerator |>>> limitChunks(2) &>> it)
      .onComplete(a => assert(Success("123456") === a))

    def limitBytes(n: Int) = {
      Traversable.take[Array[Byte]](n)
    }
    (enumerator |>>> limitBytes(5) &>> it)
      .onComplete(a => assert(Success("12345") === a))
  }
}
