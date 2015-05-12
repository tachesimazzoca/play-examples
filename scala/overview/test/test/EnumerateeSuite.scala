package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import play.api.libs.iteratee._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class EnumerateeSuite extends FunSuite with ScalaFutures {
  test("&>> is transform") {
    val consume = Iteratee.consume[String]()

    val bytes = Enumerator("Enumeratee".getBytes: _*)
    val byteToHexStr = Enumeratee.map[Byte] { b =>
      "%02X".format(b)
    }

    //val result = bytes |>>> byteToHexStr.transform(consume)
    val result = bytes |>>> (byteToHexStr &>> consume)
    whenReady(result) { a =>
      assert("456E756D657261746565" === a)
    }
  }

  test("with Enumerator#(through|&>)") {
    val sum = Iteratee.fold[Int, Int](0) { (acc, x) =>
      acc + x
    }

    val strings = Enumerator("1", "2", "3")
    val strToInt = Enumeratee.map[String](_.toInt)

    //val result = strings.through(strToInt) |>>> sum
    val result = (strings &> strToInt) |>>> sum
    whenReady(result) { a =>
      assert(6 === a)
    }
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
    whenReady(Enumerator("3", "4", "5") |>>> transformedIt) { a =>
      assert(3 === a)
    }

    // The method apply returns Iteratee[String, Iteratee[Int, Int]]
    val adaptedIt = strToInt(sum)
    // so we can get the original iteratee after the adaptedIt is done.
    val originalIt = Iteratee.flatten(Enumerator("1", "2") |>>> adaptedIt)
    // The original iteratee has not been done yet because it's just
    // an output of the adaptedIt.
    Iteratee.isDoneOrError(originalIt).onComplete(a => assert(a === Success(false)))
    whenReady(Enumerator(3, 4, 5) |>>> originalIt) { a =>
      assert(15 === a)
    }
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
    whenReady(result) { a =>
      assert(6 === a)
    }
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
    whenReady(enumerator |>>> limitChunks(2) &>> it) { a =>
      assert("123456" === a)
    }

    def limitBytes(n: Int) = {
      Traversable.take[Array[Byte]](n)
    }
    whenReady(enumerator |>>> limitBytes(5) &>> it) { a =>
      assert("12345" === a)
    }
  }
}
