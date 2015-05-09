package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.http.Writeable
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class BodyParserSuite extends FunSuite {
  val bytesConsumer = Iteratee.consume[Array[Byte]]()

  def byteToStr(bytes: Array[Byte]): String = bytes.map(_.toChar).mkString("")

  def parsedIt[A](parser: BodyParser[A])(rh: RequestHeader)
                 (implicit w: Writeable[A]): Iteratee[Array[Byte], Result] =
    parser(rh) mapM {
      case Left(r) =>
        Future.successful(r)
      case Right(a) =>
        val req = Request(rh, a)
        Future.successful(Ok(a))
    }

  def parseBody(chunk: Enumerator[Array[Byte]],
                it: Iteratee[Array[Byte], Result]): Future[(Int, String)] = {
    (for {
      r1 <- chunk |>>> it
      r2 <- r1.body |>>> bytesConsumer
    } yield (r1.header.status, r2)).map(t => (t._1, byteToStr(t._2)))
  }

  test("parse.text") {
    val body = "foo"
    val headers = Seq(
      "Content-Type" -> Seq("text/plain; charset=utf-8")
    )
    val rh = FakeRequest("POST", "/submit", FakeHeaders(headers), "")
    val it = parsedIt(parse.text)(rh)

    parseBody(Enumerator(body.getBytes()), it)
      .onComplete(a => assert(Success((200, body)) === a))

    Thread.sleep(500L)
  }

  test("parse.xml") {
    val body = "<foo>bar</foo>"
    val headers = Seq(
      "Content-type" -> Seq("application/xml")
    )
    val rh = FakeRequest("POST", "/submit", FakeHeaders(headers), "")
    val it = parsedIt(parse.xml)(rh)

    parseBody(Enumerator(body.getBytes()), it)
      .onComplete(a => assert(Success((200, body)) === a))

    Thread.sleep(500L)
  }

  test("parse.json") {
    val body = "[1,2,3]"
    val headers = Seq(
      "Content-type" -> Seq("application/json")
    )
    val rh = FakeRequest("POST", "/submit", FakeHeaders(headers), "")
    val it = parsedIt(parse.json)(rh)

    parseBody(Enumerator(body.getBytes()), it)
      .onComplete(a => assert(Success((200, body)) === a))

    Thread.sleep(500L)
  }
}
