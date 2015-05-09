package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.http.Writeable
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class BodyParserSuite extends FunSuite {

  case class DummyRequestHeader(headersMap: Map[String, Seq[String]] = Map())
    extends RequestHeader {
    def id = 1

    def tags = Map()

    def uri = ""

    def path = ""

    def method = ""

    def version = ""

    def queryString = Map()

    def remoteAddress = ""

    def secure = false

    lazy val headers = new Headers {
      val data = headersMap.toSeq
    }
  }

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
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("text/plain")))
    val it = parse.text(rh)

    (Enumerator(body.getBytes()) |>>> it)
      .onComplete {
      case Success(Right(a)) => assert(body === a)
      case _ => fail("it must be Success(Right(a))")
    }

    Thread.sleep(500L)
  }

  test("parse.xml") {
    val body = "<foo>bar</foo>"
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("application/xml")))
    val it = parsedIt(parse.xml)(rh)

    parseBody(Enumerator(body.getBytes()), it)
      .onComplete(a => assert(Success((200, body)) === a))

    parseBody(Enumerator("<dead>beef</".getBytes()), it)
      .onComplete(a => assert(Success((400, "")) === a))

    Thread.sleep(500L)
  }

  test("parse.json") {
    val body = "[1,2,3]"
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("application/json")))
    val it = parsedIt(parse.json)(rh)

    parseBody(Enumerator(body.getBytes()), it)
      .onComplete(a => assert(Success((200, body)) === a))

    parseBody(Enumerator("dead -> beef!".getBytes()), it)
      .onComplete(a => assert(Success((400, "")) === a))

    Thread.sleep(500L)
  }
}
