package test

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import play.api.http.Writeable
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class BodyParserSuite extends FunSuite with ScalaFutures {

  private case class DummyRequestHeader(headersMap: Map[String, Seq[String]] = Map())
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

  private val bytesConsumer = Iteratee.consume[Array[Byte]]()

  private def withSource[A](source: Source)(block: Source => A): A = {
    try {
      block(source)
    } finally {
      source.close()
    }
  }

  private def byteToStr(bytes: Array[Byte]): String = bytes.map(_.toChar).mkString("")

  private def parsedIt[A](parser: BodyParser[A])(rh: RequestHeader)
                         (implicit w: Writeable[A]): Iteratee[Array[Byte], Result] =
    parser(rh) mapM {
      case Left(r) =>
        Future.successful(r)
      case Right(a) =>
        Future.successful(Ok(a))
    }

  private def parseBody(chunk: Enumerator[Array[Byte]],
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

    whenReady(Enumerator(body.getBytes) |>>> it) {
      case Right(a) => assert(body === a)
      case _ => fail("it must be Success(Right(a))")
    }
  }

  test("parse.anyContent") {
    val body = <foo>bar</foo>
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("application/xml")))
    val it = parse.anyContent(rh)

    whenReady(Enumerator(body.toString().getBytes) |>>> it) {
      case Right(a) => assert(Some(body) === a.asXml)
      case _ => fail("it must be Success(Right(a))")
    }
  }

  test("parse.xml") {
    val body = "<foo>bar</foo>"
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("application/xml")))
    val it = parsedIt(parse.xml)(rh)

    whenReady(parseBody(Enumerator(body.getBytes), it)) { a =>
      assert((200, body) === a)
    }

    whenReady(parseBody(Enumerator("<dead>beef</".getBytes), it)) { a =>
      assert((400, "") === a)
    }
  }

  test("parse.json") {
    val body = "[1,2,3]"
    val rh = DummyRequestHeader(Map("Content-Type" -> Seq("application/json")))
    val it = parsedIt(parse.json)(rh)

    whenReady(parseBody(Enumerator(body.getBytes), it)) { a =>
      assert((200, body) === a)
    }

    whenReady(parseBody(Enumerator("dead -> beef!".getBytes), it)) { a =>
      assert((400, "") === a)
    }
  }

  test("parse.file") {
    val body = "Using parse.file"
    val output = java.io.File.createTempFile("bodyparsersuite-", "")
    val it = parse.file(to = output)(FakeRequest())

    whenReady(Enumerator(body.getBytes) |>>> it) { x =>
      x match {
        case Right(a) =>
          val actual = withSource(Source.fromFile(a)) { s =>
            s.getLines().toSeq.mkString("")
          }
          assert(body === actual)
        case _ =>
          fail("it must be Success(Right(a))")
      }
      output.delete()
    }
  }

  test("parse.temporaryFile") {
    val body = "Using parse.temporaryFile"
    val it = parse.temporaryFile(FakeRequest())

    whenReady(Enumerator(body.getBytes) |>>> it) {
      case Right(a) =>
        val actual = withSource(Source.fromFile(a.file)) { s =>
          s.getLines().toSeq.mkString("")
        }
        assert(body === actual)
        a.file.delete()
      case _ => fail("it must be Success(Right(a))")
    }
  }

  test("parse.multipartFormData") {
    val rh = DummyRequestHeader(
      Map("Content-Type" -> Seq("multipart/form-data; boundary=AaB03x")))

    val body = """
                 |--AaB03x
                 |content-disposition: form-data; name="id"
                 |
                 |1234
                 |--AaB03x
                 |content-disposition: form-data; name="file1"; filename="file1.txt"
                 |Content-Type: text/plain
                 |
                 |Using multipart/form-data
                 |--AaB03x--
               """.stripMargin.replace(String.format("%n"), "\r\n")
    val it = parse.maxLength(1024, parse.multipartFormData)(rh)

    whenReady(Enumerator(body.getBytes) |>>> it) {
      case Right(Right(a)) =>
        a.file("file1").map { part =>
          assert("file1.txt" === part.filename)
          val actual = withSource(Source.fromFile(part.ref.file)) { s =>
            s.getLines().toSeq.mkString("")
          }
          assert("Using multipart/form-data" === actual)
          part.ref.file.delete()
        }
      case _ => fail("it must be Success(Right(a))")
    }
  }
}
